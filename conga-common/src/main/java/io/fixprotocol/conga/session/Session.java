/*
 * Copyright 2018 FIX Protocol Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package io.fixprotocol.conga.session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.fixprotocol.conga.messages.MessageException;

/**
 * Abstract FIXP session, independent of encoding
 * <p>
 * Limitation: does not support FIXP multiplexing. In other words, there is a 1:1 relationship
 * between Session and transport.
 * 
 * @author Don Mendelson
 */
public abstract class Session {

  public static abstract class Builder<T> {

    public SessionMessageConsumer sessionMessageConsumer;
    public long heartbeatInterval;
    public byte[] sessionId = new byte[16];
    public Timer timer;
    public FlowType inboundFlowType = FlowType.IDEMPOTENT;
    public FlowType outboundFlowType = FlowType.IDEMPOTENT;

    protected Builder() {

    }
    
    public abstract T build();
    
    public Builder<T> heartbeatInterval(long heartbeatInterval) {
      this.heartbeatInterval = heartbeatInterval;
      return this;
    }

    public Builder<T> inboundFlowType(
        FlowType inboundFlowType) {
      this.inboundFlowType = inboundFlowType;
      return this;
    }

    public Builder<T> outboundFlowType(
        FlowType outboundFlowType) {
      this.outboundFlowType = outboundFlowType;
      return this;
    }

    public Builder<T> sessionId(byte[] sessionId) {
      this.sessionId = sessionId;
      return this;
    }

    public Builder<T> sessionMessageConsumer(
        SessionMessageConsumer sessionMessageConsumer) {
      this.sessionMessageConsumer = sessionMessageConsumer;
      return this;
    }

    public Builder<T> timer(Timer timer) {
      this.timer = timer;
      return this;
    }

  }

  public enum FlowType {
    /**
     * Guarantees at-most-once delivery
     */
    IDEMPOTENT,

    /**
     * No application messages should be sent in one direction of a session
     */
    NONE,

    /**
     * Guarantees exactly-once message delivery
     */
    RECOVERABLE,

    /**
     * Best effort delivery
     */
    UNSEQUENCED
  }

  public enum MessageType {
    /**
     * A new application message
     */
    APPLICATION,
    /**
     * Notification that one or more messages were missed
     */
    NOT_APPLIED,
    /**
     * A retransmitted application message
     */
    RETRANSMISSION,
    /**
     * A request to retransmit one or more missed messages
     */
    RETRANSMIT_REQUEST,
    /**
     * Sequence message, used as a heartbeat
     */
    SEQUENCE,
    /**
     * Unknown message type
     */
    UNKNOWN
  }

  private class HeartbeatDueTask extends TimerTask {

    @Override
    public void run() {
      if (isHeartbeatDueToReceive()) {
        doDisconnect();
      }

    }
  }
  private class HeartbeatSendTask extends TimerTask {

    @Override
    public void run() {
      if (isHeartbeatDueToSend()) {
        try {
          doSendHeartbeat(nextSeqNoSent.get());
        } catch (IOException | InterruptedException | IllegalStateException e) {
          disconnected();
        }
      }

    }
  };

  /**
   * Serialize a session ID
   * 
   * @param uuid unique session ID
   * @return a populated byte array
   */
  public static byte[] UUIDAsBytes(UUID uuid) {
    Objects.requireNonNull(uuid);
    final byte[] sessionId = new byte[16];
    // UUID is big-endian according to standard, which is the default byte
    // order of ByteBuffer
    final ByteBuffer b = ByteBuffer.wrap(sessionId);
    b.putLong(0, uuid.getMostSignificantBits());
    b.putLong(8, uuid.getLeastSignificantBits());
    return sessionId;
  }

  private final SessionMessageConsumer sessionMessageConsumer;;
  private final AtomicBoolean criticalSection = new AtomicBoolean();
  private HeartbeatDueTask heartbeatDueTask;
  private final long heartbeatInterval;
  private HeartbeatSendTask heartbeatSendTask;
  private FlowType inboundFlowType;
  private boolean isConnected = false;
  private final AtomicBoolean isHeartbeatDueToReceive = new AtomicBoolean(true);
  private final AtomicBoolean isHeartbeatDueToSend = new AtomicBoolean(true);
  private final AtomicLong nextSeqNoAccepted = new AtomicLong(1);
  private final AtomicLong nextSeqNoReceived = new AtomicLong(1L);
  private final AtomicLong nextSeqNoSent = new AtomicLong(1L);
  private FlowType outboundFlowType;
  private final byte[] sessionId;
  private final Timer timer;
  private String principal;

  protected Session(@SuppressWarnings("rawtypes") Builder builder) {
    this.timer = builder.timer;
    this.heartbeatInterval = builder.heartbeatInterval;
    this.sessionId = builder.sessionId;
    this.sessionMessageConsumer = builder.sessionMessageConsumer;
    this.inboundFlowType = builder.inboundFlowType;
    this.outboundFlowType = builder.outboundFlowType;
  }

  /**
   * Returns sequence number of received application message Side effect: sequence number is
   * incremented
   * 
   * @return sequence number
   */
  public long applicationMessageReceived() {
    return nextSeqNoReceived.getAndIncrement();
  }

  /**
   * Returns sequence number of sent application message Side effect: sequence number is incremented
   * 
   * @return sequence number
   */
  public long applicationMessageSent() {
    return nextSeqNoSent.getAndIncrement();
  }

  /**
   * The underlying transport was connected
   * @param principal party responsible for this Session
   * 
   * @return Returns {@code true} if the transport was previously unconnected, {@code false} if it
   *         was already connected.
   */
  public boolean connected(Object transport, String principal) {
    while (!criticalSection.compareAndSet(false, true)) {
      Thread.yield();
    }
    try {
      if (!isConnected) {
        this.principal = principal;
        // Give 10% margin
        long heartbeatDueInterval = (heartbeatInterval * 11) / 10;
        heartbeatDueTask = new HeartbeatDueTask();
        timer.scheduleAtFixedRate(heartbeatDueTask, heartbeatDueInterval, heartbeatDueInterval);
        heartbeatSendTask = new HeartbeatSendTask();
        timer.scheduleAtFixedRate(heartbeatSendTask, heartbeatInterval, heartbeatInterval);
        isConnected = true;
        return true;
      } else {
        return false;
      }
    } finally {
      criticalSection.compareAndSet(true, false);
    }
  }


  /**
   * The underlying transport was disconnected
   * 
   * @return Returns {@code true} if the transport was previously connected, {@code false} if it was
   *         already disconnected.
   */

  public boolean disconnected() {
    while (!criticalSection.compareAndSet(false, true)) {
      Thread.yield();
    }
    try {
      if (isConnected) {
        heartbeatDueTask.cancel();
        heartbeatSendTask.cancel();
        isConnected = false;
        return true;
      } else {
        return false;
      }
    } finally {
      criticalSection.compareAndSet(true, false);
    }
  }

  public FlowType getInboundFlowType() {
    return inboundFlowType;
  }

  public long getNextSeqNoReceived() {
    return nextSeqNoReceived.get();
  }

  public long getNextSeqNoSent() {
    return nextSeqNoSent.get();
  }

  public FlowType getOutboundFlowType() {
    return outboundFlowType;
  }

  public String getPrincipal() {
    return principal;
  }

  public byte[] getSessionId() {
    return sessionId;
  }

  public boolean isConnected() {
    while (!criticalSection.compareAndSet(false, true)) {
      Thread.yield();
    }
    try {
      return isConnected;
    } finally {
      criticalSection.compareAndSet(true, false);
    }
  }

  /**
   * Notifies the Session that a message has been received
   * @param buffer holds a single message
   * @throws ProtocolViolationException if the Session is in an invalid state for the message type
   * @throws MessageException if the message cannot be parsed or its type is unknown
   */
  public void messageReceived(ByteBuffer buffer) throws ProtocolViolationException, MessageException {
    isHeartbeatDueToReceive.set(false);
    MessageType messageType = getMessageType(buffer);

    long seqNo;
    switch (messageType) {
      case SEQUENCE:
        sequenceMessageReceived(buffer);
        break;
      case APPLICATION:
        if (inboundFlowType == FlowType.NONE) {
          throw new ProtocolViolationException("Application message received on NONE flow");
        }
        seqNo = applicationMessageReceived();
        if (nextSeqNoAccepted.compareAndSet(seqNo, seqNo)) {
          nextSeqNoAccepted.incrementAndGet();
          sessionMessageConsumer.accept(principal, buffer, seqNo);
        } 
        // else duplicate message ignored
        break;
      case NOT_APPLIED:
        if (outboundFlowType != FlowType.IDEMPOTENT) {
          throw new ProtocolViolationException(
              "NotApplied message received for non-Idempotent flow");
        }
        // Session protocol defined message but consumes a sequence number
        seqNo = applicationMessageReceived();
        if (nextSeqNoAccepted.compareAndSet(seqNo, seqNo)) {
          nextSeqNoAccepted.incrementAndGet();
          sessionMessageConsumer.accept(principal, buffer, seqNo);
        } 
      default:
        throw new MessageException("Unknown message type received");
    }
  }

  /**
   * Send an application message
   * 
   * @param buffer buffer containing a message
   * @return the sequence number of the sent message or {@code 0} if an error occurs
   * @throws IOException if an IO error occurs
   * @throws InterruptedException if the operation is interrupted before completion
   * @throws IllegalStateException if the transport is not connected
   */
  public long sendApplicationMessage(ByteBuffer buffer) throws IOException, InterruptedException {
    long seqNo = 0;
    try {
      if (isConnected()) {
        doSendMessage(buffer);
        seqNo = applicationMessageSent();
        isHeartbeatDueToSend.set(false);
      } else {
        throw new IllegalStateException("Not connected");
      }
    } catch (IOException e) {
      disconnected();
      throw e;
    }
    return seqNo;
  }

  /**
   * Send a FIXP Sequence message
   * <p>
   * Design note: WebSocket has Ping/Pong messages for keep-alive. However, behavior is not well
   * defined by the specification for frequency or content. Therefore, it was not overloaded for the
   * purpose of idempotent message delivery.
   */
  public void sendHeartbeat() {
    try {
      doSendHeartbeat(getNextSeqNoSent());
    } catch (IOException | InterruptedException e) {
      disconnected();
    }
  }

  public void setInboundFlowType(FlowType inboundFlowType) {
    this.inboundFlowType = inboundFlowType;
  }

  public void setOutboundFlowType(FlowType outboundFlowType) {
    this.outboundFlowType = outboundFlowType;
  }

  private boolean isHeartbeatDueToReceive() {
    return isHeartbeatDueToReceive.getAndSet(true);
  }

  private boolean isHeartbeatDueToSend() {
    return isHeartbeatDueToSend.getAndSet(true);
  }

  private void sequenceMessageReceived(ByteBuffer buffer) {
    long newNextSeqNo = getNextSequenceNumber(buffer);
    long prevNextSeqNo = nextSeqNoReceived.getAndSet(newNextSeqNo);
    long accepted = nextSeqNoAccepted.get();
    if (newNextSeqNo > accepted) {
      try {
        switch (inboundFlowType) {
          case RECOVERABLE:
            doSendRetransmitRequest(prevNextSeqNo, newNextSeqNo - prevNextSeqNo);
            break;
          case IDEMPOTENT:
            doSendNotApplied(prevNextSeqNo, newNextSeqNo - prevNextSeqNo);
            break;
          default:
            // do nothing
            break;
        }
        nextSeqNoAccepted.set(newNextSeqNo);
      } catch (IOException | InterruptedException e) {
        disconnected();
      }
    }
  }

  protected abstract void doDisconnect();

  protected abstract void doSendHeartbeat(long nextSeqNo) throws IOException, InterruptedException;

  protected abstract void doSendMessage(ByteBuffer buffer) throws IOException, InterruptedException;

  protected abstract void doSendNotApplied(long fromSeqNo, long count)
      throws IOException, InterruptedException;

  protected abstract void doSendRetransmitRequest(long fromSeqNo, long count)
      throws IOException, InterruptedException;

  protected abstract MessageType getMessageType(ByteBuffer buffer);

  protected abstract long getNextSequenceNumber(ByteBuffer buffer);

  /**
   * A time relative to an internal clock; not guaranteed to be wall clock time.
   * 
   * @return current timestamp as nanoseconds
   */
  protected long getTimeAsNanos() {
    return System.nanoTime();
  }
}
