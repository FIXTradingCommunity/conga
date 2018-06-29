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
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Executor;
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

  public abstract static class Builder<T> {

    public Executor executor;
    private long heartbeatInterval;
    private FlowType inboundFlowType = FlowType.IDEMPOTENT;
    private FlowType outboundFlowType = FlowType.IDEMPOTENT;
    private List<ByteBuffer> sendCache;
    private byte[] sessionId = new byte[16];
    private SessionMessageConsumer sessionMessageConsumer = null;
    private Timer timer;

    protected Builder() {

    }

    public abstract T build();

    /**
     * Executor for asynchronous operations
     * 
     * @param executor executor implementation
     * @return this Builder
     */
    public Builder<T> executor(Executor executor) {
      this.executor = executor;
      return this;
    }

    /**
     * Heartbeat interval in millis
     * 
     * @param heartbeatInterval interval in millis
     * @return this Builder
     */
    public Builder<T> heartbeatInterval(long heartbeatInterval) {
      this.heartbeatInterval = heartbeatInterval;
      return this;
    }

    /**
     * Delivery guarantee for inbound messages
     * 
     * @param inboundFlowType inbound flow type
     * @return this Builder
     */
    public Builder<T> inboundFlowType(FlowType inboundFlowType) {
      this.inboundFlowType = inboundFlowType;
      return this;
    }

    /**
     * Delivery guarantee for outbound messages
     * 
     * @param inboundFlowType outbound flow type
     * @return this Builder
     */
    public Builder<T> outboundFlowType(FlowType outboundFlowType) {
      this.outboundFlowType = outboundFlowType;
      return this;
    }

    /**
     * An interface to persist outbound messages for recoverability.
     *
     * @param sendCache an interface to persist messages
     * @return this Builder
     */
    public Builder<T> sendCache(List<ByteBuffer> sendCache) {
      this.sendCache = sendCache;
      return this;
    }

    /**
     * A transient FIXP session identifier
     * 
     * @param sessionId session identifier in the form of a UUID
     * @return this Builder
     */
    public Builder<T> sessionId(byte[] sessionId) {
      this.sessionId = sessionId;
      return this;
    }

    /**
     * A consumer of messages received through this Session
     * 
     * @param sessionMessageConsumer a message consumer
     * @return this Builder
     */
    public Builder<T> sessionMessageConsumer(SessionMessageConsumer sessionMessageConsumer) {
      this.sessionMessageConsumer = sessionMessageConsumer;
      return this;
    }

    /**
     * A shared Timer for scheduled events
     * 
     * @param timer
     * @return this Builder
     */
    public Builder<T> timer(Timer timer) {
      this.timer = timer;
      return this;
    }

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
  }

  protected static class SequenceRange {
    private long count;
    private long fromSeqNo;
    private long timestamp;

    public long getCount() {
      return count;
    }

    public long getFromSeqNo() {
      return fromSeqNo;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public void setCount(long count) {
      this.count = count;
    }

    public void setFromSeqNo(long fromSeqNo) {
      this.fromSeqNo = fromSeqNo;
    }

    public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
    }
  }

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

  /**
   * A time relative to an internal clock; not guaranteed to be wall clock time.
   * 
   * @return current timestamp as nanoseconds
   */
  protected static long getTimeAsNanos() {
    return System.nanoTime();
  }

  private final AtomicBoolean connectedCriticalSection = new AtomicBoolean();
  private final Executor executor;
  private HeartbeatDueTask heartbeatDueTask;
  private final long heartbeatInterval;
  private HeartbeatSendTask heartbeatSendTask;
  private FlowType inboundFlowType;
  private boolean isConnected = false;
  private final AtomicBoolean isHeartbeatDueToReceive = new AtomicBoolean(true);
  private final AtomicBoolean isHeartbeatDueToSend = new AtomicBoolean(true);
  private boolean isRetransmission = false;
  private long nextSeqNoAccepted = 1L;
  private long nextSeqNoReceived = 1L;
  private final AtomicLong nextSeqNoSent = new AtomicLong(1L);
  private FlowType outboundFlowType;
  private String principal;
  private final AtomicBoolean receivedCriticalSection = new AtomicBoolean();
  private SequenceRange retransmitRange = new SequenceRange();
  private List<ByteBuffer> sendCache;
  private final AtomicBoolean sendCriticalSection = new AtomicBoolean();
  private final byte[] sessionId;
  private final SessionMessageConsumer sessionMessageConsumer;
  private final Timer timer;

  protected Session(@SuppressWarnings("rawtypes") Builder builder) {
    this.timer = builder.timer;
    this.heartbeatInterval = builder.heartbeatInterval;
    this.sessionId = builder.sessionId;
    this.sessionMessageConsumer = builder.sessionMessageConsumer;
    this.inboundFlowType = builder.inboundFlowType;
    this.outboundFlowType = builder.outboundFlowType;
    this.sendCache = builder.sendCache;
    this.executor = builder.executor;

    if (outboundFlowType == FlowType.RECOVERABLE && sendCache == null) {
      throw new IllegalArgumentException("No send cache for a recoverable flow");
    }
    if (outboundFlowType == FlowType.RECOVERABLE && executor == null) {
      throw new IllegalArgumentException("No Executor for asynchronous retransmission");
    }
  }

  /**
   * Returns sequence number of received application message Side effect: sequence number is
   * incremented
   * 
   * @param buffer holds an application message
   */
  public void applicationMessageReceived(ByteBuffer buffer) {
    long seqNo = nextSeqNoReceived;
    nextSeqNoReceived++;
    if (nextSeqNoAccepted == seqNo) {
      sessionMessageConsumer.accept(principal, buffer, seqNo);
      nextSeqNoAccepted++;
    }
    // else duplicate message ignored
  }

  /**
   * The underlying transport was connected
   * 
   * @param principal party responsible for this Session
   * 
   * @return Returns {@code true} if the transport was previously unconnected, {@code false} if it
   *         was already connected.
   */
  public boolean connected(Object transport, String principal) {
    while (!connectedCriticalSection.compareAndSet(false, true)) {
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
      connectedCriticalSection.compareAndSet(true, false);
    }
  }

  /**
   * The underlying transport was disconnected
   * 
   * @return Returns {@code true} if the transport was previously connected, {@code false} if it was
   *         already disconnected.
   */

  public boolean disconnected() {
    while (!connectedCriticalSection.compareAndSet(false, true)) {
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
      connectedCriticalSection.compareAndSet(true, false);
    }
  }

  public FlowType getInboundFlowType() {
    return inboundFlowType;
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
    while (!connectedCriticalSection.compareAndSet(false, true)) {
      Thread.yield();
    }
    try {
      return isConnected;
    } finally {
      connectedCriticalSection.compareAndSet(true, false);
    }
  }

  /**
   * Notifies the Session that a message has been received
   * 
   * @param buffer holds a single message
   * @throws ProtocolViolationException if the Session is in an invalid state for the message type
   * @throws MessageException if the message cannot be parsed or its type is unknown
   */
  public void messageReceived(ByteBuffer buffer)
      throws ProtocolViolationException, MessageException {
    while (!receivedCriticalSection.compareAndSet(false, true)) {
      Thread.yield();
    }
    try {
      isHeartbeatDueToReceive.set(false);
      MessageType messageType = getMessageType(buffer);

      switch (messageType) {
        case SEQUENCE:
          sequenceMessageReceived(buffer);
          break;
        case APPLICATION:
          if (FlowType.NONE == inboundFlowType) {
            throw new ProtocolViolationException("Application message received on NONE flow");
          }
          applicationMessageReceived(buffer);
          break;
        case NOT_APPLIED:
          if (outboundFlowType != FlowType.IDEMPOTENT) {
            throw new ProtocolViolationException(
                "NotApplied message received for non-Idempotent flow");
          }
          // Session protocol defined message but consumes a sequence number
          applicationMessageReceived(buffer);
          break;
        case RETRANSMIT_REQUEST:
          if (outboundFlowType != FlowType.RECOVERABLE) {
            throw new ProtocolViolationException(
                "Retransmit request message received for non-Recoverable flow");
          }
          getRetransmitRequestSequenceRange(buffer, retransmitRange);
          retransmitAsync(retransmitRange);
          break;
        case RETRANSMISSION:
          if (inboundFlowType != FlowType.RECOVERABLE) {
            throw new ProtocolViolationException("Retransmission received on non-Recoverable flow");
          }
          isRetransmission = true;
          getRetransmissionSequenceRange(buffer, retransmitRange);
          break;
        default:
          throw new MessageException("Unknown message type received");
      }
    } finally {
      receivedCriticalSection.compareAndSet(true, false);
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
    if (isConnected()) {
      try {
        while (!sendCriticalSection.compareAndSet(false, true)) {
          Thread.yield();
        }

        doSendMessage(buffer);
        seqNo = nextSeqNoSent.getAndIncrement();
        isHeartbeatDueToSend.set(false);
        if (outboundFlowType == FlowType.RECOVERABLE) {
          sendCache.add((int) seqNo, buffer);
        }

      } catch (IOException e) {
        disconnected();
        throw e;
      } finally {
        sendCriticalSection.compareAndSet(true, false);
      }
    } else {
      throw new IllegalStateException("Not connected");
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
      doSendHeartbeat(nextSeqNoSent.get());
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

  private void retransmitAsync(SequenceRange retransmitRange) {
    Objects.requireNonNull(retransmitRange);

    executor.execute(() -> {
      List<ByteBuffer> buffers = sendCache.subList((int) retransmitRange.fromSeqNo,
          (int) (retransmitRange.fromSeqNo + retransmitRange.count));
      try {
        while (!sendCriticalSection.compareAndSet(false, true)) {
          Thread.yield();
        }
        doSendRetransmission(retransmitRange);
        for (ByteBuffer buffer : buffers) {
          doSendMessage(buffer);
        }
      } catch (IOException e) {
        disconnected();
      } catch (InterruptedException e) {

      } finally {
        sendCriticalSection.compareAndSet(true, false);
      }
    });
  }

  private void sequenceMessageReceived(ByteBuffer buffer) {
    isRetransmission = false;
    long newNextSeqNo = getNextSequenceNumber(buffer);
    long prevNextSeqNo = nextSeqNoReceived;
    nextSeqNoReceived = newNextSeqNo;
    if (newNextSeqNo > nextSeqNoAccepted) {
      try {
        switch (inboundFlowType) {
          case RECOVERABLE:
            retransmitRange.setFromSeqNo(prevNextSeqNo);
            retransmitRange.setTimestamp(getTimeAsNanos());
            retransmitRange.setCount(newNextSeqNo - prevNextSeqNo);
            doSendRetransmitRequest(retransmitRange);
            break;
          case IDEMPOTENT:
            doSendNotApplied(prevNextSeqNo, newNextSeqNo - prevNextSeqNo);
            break;
          default:
            // do nothing
            break;
        }
        nextSeqNoAccepted = newNextSeqNo;
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

  protected abstract void doSendRetransmission(SequenceRange range)
      throws IOException, InterruptedException;

  protected abstract void doSendRetransmitRequest(SequenceRange range)
      throws IOException, InterruptedException;

  protected abstract MessageType getMessageType(ByteBuffer buffer);

  protected abstract long getNextSequenceNumber(ByteBuffer buffer);

  protected abstract void getRetransmissionSequenceRange(ByteBuffer buffer, SequenceRange range);

  protected abstract void getRetransmitRequestSequenceRange(ByteBuffer buffer, SequenceRange range);
}
