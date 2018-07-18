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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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

    public byte[] credentials;
    public long lastSeqNoReceived = 0;
    private Executor executor;
    private long heartbeatInterval;
    private FlowType outboundFlowType = FlowType.Idempotent;
    private List<ByteBuffer> sendCache;
    private byte[] sessionId = new byte[16];
    private SessionMessageConsumer sessionMessageConsumer = null;
    private Timer timer;;

    protected Builder() {

    }

    public abstract T build();

    /**
     * Credentials for authentication
     * 
     * @param credentials for authentication
     * @return this Builder
     */
    public Builder<T> credentials(byte[] credentials) {
      Objects.requireNonNull(credentials);
      this.credentials = credentials;
      return this;
    }

    /**
     * Executor for asynchronous operations
     * 
     * @param executor executor implementation
     * @return this Builder
     */
    public Builder<T> executor(Executor executor) {
      Objects.requireNonNull(executor);
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
     * If rebuilding a failed session, set the sequence number of the last appplication message
     * received.
     * 
     * @param lastSeqNoReceived sequence number of last message received
     * @return this Builder
     */
    public Builder<T> lastSeqNoReceived(long lastSeqNoReceived) {
      this.lastSeqNoReceived = lastSeqNoReceived;
      return this;
    }

    /**
     * Delivery guarantee for outbound messages
     * 
     * @param outboundFlowType outbound flow type
     * @return this Builder
     */
    public Builder<T> outboundFlowType(FlowType outboundFlowType) {
      Objects.requireNonNull(outboundFlowType);
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
      Objects.requireNonNull(sendCache);
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
      Objects.requireNonNull(sessionId);
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
      Objects.requireNonNull(sessionMessageConsumer);
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
      Objects.requireNonNull(timer);
      this.timer = timer;
      return this;
    }

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
          doSendSequence(nextSeqNoSent.get());
        } catch (IOException | InterruptedException | IllegalStateException e) {
          disconnected();
        }
      }
    }
  }

  protected enum MessageType {
    /**
     * A new application message
     */
    APPLICATION,
    /**
     * Establish request message
     */
    ESTABLISH,
    /**
     * Establishment response message
     */
    ESTABLISHMENT_ACK,
    /**
     * Establishment rejection message
     */
    ESTABLISHMENT_REJECT,
    /**
     * Negotiate request message
     */
    NEGOTIATE,
    /**
     * Negotiation rejected
     */
    NEGOTIATION_REJECT,
    /**
     * Negotiation accepted
     */
    NEGOTIATION_RESPONSE,
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
  private byte[] credentials;
  private SubmissionPublisher<SessionEvent> eventPublisher;
  private final Executor executor;
  private long heartbeatDueInterval;
  private HeartbeatDueTask heartbeatDueTask;
  private final long heartbeatInterval;
  private HeartbeatSendTask heartbeatSendTask;
  private FlowType inboundFlowType;
  // if false, then is server session
  private boolean isConnected = false;
  private final AtomicBoolean isHeartbeatDueToReceive = new AtomicBoolean(true);
  private final AtomicBoolean isHeartbeatDueToSend = new AtomicBoolean(true);
  private boolean isReceivingRetransmission = false;
  private boolean isSendingRetransmission = false;
  private long lastRequestTimestamp = 0;
  private long lastRetransSeqNoToAccept;
  private long nextRetransSeqNoReceived;
  // high water mark
  private long nextSeqNoAccepted = 1L;
  private long nextSeqNoReceived = 1L;
  private final AtomicLong nextSeqNoSent = new AtomicLong(1L);
  private FlowType outboundFlowType;
  private String principal;
  private final AtomicBoolean receivedCriticalSection = new AtomicBoolean();
  private SequenceRange retransmitRange = new SequenceRange();
  private List<ByteBuffer> sendCache;
  private final AtomicBoolean sendCriticalSection = new AtomicBoolean();
  private byte[] sessionId;
  private final SessionMessageConsumer sessionMessageConsumer;
  private AtomicReference<SessionState> sessionState =
      new AtomicReference<>(SessionState.NOT_NEGOTIATED);
  private final Timer timer;

  protected Session(Builder<?> builder) {
    this.timer = builder.timer;
    this.heartbeatInterval = builder.heartbeatInterval;
    this.sessionId = builder.sessionId;
    this.sessionMessageConsumer = builder.sessionMessageConsumer;
    this.outboundFlowType = builder.outboundFlowType;
    this.sendCache = builder.sendCache;
    this.executor = builder.executor;
    this.credentials = builder.credentials;
    this.nextSeqNoReceived = builder.lastSeqNoReceived + 1;
    if (this.executor != null) {
      this.eventPublisher = new SubmissionPublisher<SessionEvent>(this.executor, 16);
    } else {
      this.eventPublisher = new SubmissionPublisher<SessionEvent>();
    }

    if (outboundFlowType == FlowType.Recoverable && sendCache == null) {
      throw new IllegalArgumentException("No send cache for a recoverable flow");
    }
    if (outboundFlowType == FlowType.Recoverable && executor == null) {
      throw new IllegalArgumentException("No Executor for asynchronous retransmission");
    }

    // Advance from index 0 in cache to first message sequence no. which is 1-based
    if (this.sendCache != null) {
      this.sendCache.add(ByteBuffer.allocate(0));
    }
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
        if (isClientSession()) {
          lastRequestTimestamp = getTimeAsNanos();
          SessionState state = getSessionState();
          switch (state) {
            case NOT_NEGOTIATED:
              doSendNegotiate(sessionId, lastRequestTimestamp, outboundFlowType, credentials);
              break;
            case NEGOTIATED:
            case NOT_ESTABLISHED:
              doSendEstablish(sessionId, lastRequestTimestamp, heartbeatInterval, nextSeqNoReceived,
                  credentials);
              break;
          }
        }
        isConnected = true;
        return true;
      } else {
        return false;
      }
    } catch (IOException | InterruptedException e) {
      // This should only occur if the WebSocket fails immediately after a connected signal.
      // Presumably, a disconnected signal would follow.
      e.printStackTrace();
      return false;
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
        cancelHearbeats();
        isConnected = false;
        setSessionState(SessionState.NOT_ESTABLISHED);
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

  public SessionState getSessionState() {
    return sessionState.get();
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

  public boolean isEstablished() {
    return sessionState.compareAndSet(SessionState.ESTABLISHED, SessionState.ESTABLISHED);
  }

  /**
   * Notifies the Session that a message has been received
   * 
   * @param buffer holds a single message
   * @throws ProtocolViolationException if the Session is in an invalid state for the message type
   * @throws MessageException if the message cannot be parsed or its type is unknown
   * @throws InterruptedException
   * @throws IOException
   */
  public void messageReceived(ByteBuffer buffer)
      throws ProtocolViolationException, MessageException, IOException, InterruptedException {
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
          if (FlowType.None == inboundFlowType) {
            throw new ProtocolViolationException("Application message received on NONE flow");
          }
          if (!isReceivingRetransmission) {
            applicationMessageReceived(buffer);
          } else {
            retransmittedApplicationMessageReceived(buffer);
          }
          break;
        case NOT_APPLIED:
          if (outboundFlowType != FlowType.Idempotent) {
            throw new ProtocolViolationException(
                "NotApplied message received for non-Idempotent flow");
          }
          // Session protocol defined message but consumes a sequence number
          applicationMessageReceived(buffer);
          break;
        case RETRANSMIT_REQUEST:
          if (outboundFlowType != FlowType.Recoverable) {
            throw new ProtocolViolationException(
                "Retransmit request message received for non-Recoverable flow");
          }
          getRetransmitRequestSequenceRange(buffer, retransmitRange);
          retransmitAsync(retransmitRange);
          break;
        case RETRANSMISSION:
          if (inboundFlowType != FlowType.Recoverable) {
            throw new ProtocolViolationException("Retransmission received on non-Recoverable flow");
          }
          retransmissionMessageReceived(buffer);
          break;
        case ESTABLISH:
          establishMessageReceived(buffer);
          break;
        case ESTABLISHMENT_ACK:
          establishmentAckMessageReceived(buffer);
          break;
        case ESTABLISHMENT_REJECT:
          establishmentRejectMessageReceived(buffer);
          break;
        case NEGOTIATE:
          negotiateMessageReceived(buffer);
          break;
        case NEGOTIATION_RESPONSE:
          negotiationResponseMessageReceived(buffer);
          break;
        case NEGOTIATION_REJECT:
          negotiationRejectMessageReceived(buffer);
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
   * @throws IllegalStateException if this Session is not established
   */
  public long sendApplicationMessage(ByteBuffer buffer) throws IOException, InterruptedException {
    Objects.requireNonNull(buffer);
    if (isEstablished()) {
      try {
        while (!sendCriticalSection.compareAndSet(false, true)) {
          Thread.yield();
        }

        long seqNo = nextSeqNoSent.getAndIncrement();
        // Cache before sending
        if (outboundFlowType == FlowType.Recoverable) {
          sendCache.add((int) seqNo, buffer);
        }

        if (isSendingRetransmission) {
          doSendSequence(seqNo);
          isSendingRetransmission = false;
        }
        isHeartbeatDueToSend.set(false);
        doSendMessage(buffer);

        return seqNo;
      } catch (IOException e) {
        disconnected();
        throw e;
      } finally {
        sendCriticalSection.compareAndSet(true, false);
      }
    } else {
      throw new IllegalStateException("Session not established");
    }

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
      doSendSequence(nextSeqNoSent.get());
    } catch (IOException | InterruptedException e) {
      disconnected();
    }
  }

  public void subscribeForEvents(Subscriber<? super SessionEvent> subscriber) {
    eventPublisher.subscribe(subscriber);
  }

  @Override
  public String toString() {
    StringBuilder builder2 = new StringBuilder();
    builder2.append("Session [isReceivingRetransmission=").append(isReceivingRetransmission)
        .append(", nextSeqNoAccepted=").append(nextSeqNoAccepted).append(", nextSeqNoReceived=")
        .append(nextSeqNoReceived).append(", nextSeqNoSent=").append(nextSeqNoSent)
        .append(", getInboundFlowType()=").append(getInboundFlowType())
        .append(", getOutboundFlowType()=").append(getOutboundFlowType())
        .append(", getPrincipal()=").append(getPrincipal()).append(", getSessionId()=")
        .append(Arrays.toString(getSessionId())).append(", isConnected()=").append(isConnected())
        .append(", isEstablished()=").append(isEstablished()).append(", isHeartbeatDueToReceive()=")
        .append(isHeartbeatDueToReceive()).append(", isHeartbeatDueToSend()=")
        .append(isHeartbeatDueToSend()).append("]");
    return builder2.toString();
  }

  private void cancelHearbeats() {
    if (heartbeatDueTask != null) {
      heartbeatDueTask.cancel();
    }
    if (heartbeatSendTask != null) {
      heartbeatSendTask.cancel();
    }
  }

  private void establishmentAckMessageReceived(ByteBuffer buffer)
      throws ProtocolViolationException {
    SessionAttributes sessionAttributes = new SessionAttributes();
    getEstablishmentAckSessionAttributes(buffer, sessionAttributes);
    if (sessionAttributes.getTimestamp() != lastRequestTimestamp) {
      throw new ProtocolViolationException(
          String.format("EstablishmentAck timestamp %d does not match last request timestamp %d",
              sessionAttributes.getTimestamp(), lastRequestTimestamp));
    }

    inboundFlowType = sessionAttributes.getFlowType();
    heartbeatDueInterval = sessionAttributes.getKeepAliveInterval();
    long expectedNextSeqNo = sessionAttributes.getNextSeqNo();

    setSessionState(SessionState.ESTABLISHED);
    if (inboundFlowType == FlowType.Recoverable && expectedNextSeqNo < nextSeqNoSent.get()) {
      SequenceRange retransmitRange = new SequenceRange();
      retransmitRange.timestamp(sessionAttributes.getTimestamp()).fromSeqNo(expectedNextSeqNo)
          .count(nextSeqNoSent.get() - expectedNextSeqNo);
      retransmitAsync(retransmitRange);
    }
    scheduleHeartbeats();
  }

  private void establishmentRejectMessageReceived(ByteBuffer buffer) {
    setSessionState(SessionState.NOT_ESTABLISHED);
  }

  private void establishMessageReceived(ByteBuffer buffer)
      throws IOException, InterruptedException {
    SessionAttributes sessionAttributes = new SessionAttributes();
    getEstablishSessionAttributes(buffer, sessionAttributes);
    sessionId = sessionAttributes.getSessionId();
    heartbeatDueInterval = sessionAttributes.getKeepAliveInterval();
    long expectedNextSeqNo = sessionAttributes.getNextSeqNo();
    final long timestamp = sessionAttributes.getTimestamp();
    doSendEstablishAck(sessionId, timestamp, heartbeatInterval, nextSeqNoReceived);
    setSessionState(SessionState.ESTABLISHED);
    if (inboundFlowType == FlowType.Recoverable && expectedNextSeqNo < nextSeqNoSent.get()) {
      SequenceRange retransmitRange = new SequenceRange();
      retransmitRange.timestamp(timestamp).fromSeqNo(expectedNextSeqNo)
          .count(nextSeqNoSent.get() - expectedNextSeqNo);
      retransmitAsync(retransmitRange);
    }
    scheduleHeartbeats();
  }

  private boolean isHeartbeatDueToReceive() {
    return isHeartbeatDueToReceive.getAndSet(true);
  }

  private boolean isHeartbeatDueToSend() {
    return isHeartbeatDueToSend.getAndSet(true);
  }

  private void negotiateMessageReceived(ByteBuffer buffer)
      throws IOException, InterruptedException {
    SessionAttributes sessionAttributes = new SessionAttributes();
    getNegotiateSessionAttributes(buffer, sessionAttributes);
    sessionId = sessionAttributes.getSessionId();
    inboundFlowType = sessionAttributes.getFlowType();
    final long timestamp = sessionAttributes.getTimestamp();
    doSendNegotiationResponse(sessionId, timestamp, outboundFlowType, null);
    setSessionState(SessionState.NEGOTIATED);
  }

  private void negotiationRejectMessageReceived(ByteBuffer buffer) {
    setSessionState(SessionState.NOT_NEGOTIATED);
  }

  private void negotiationResponseMessageReceived(ByteBuffer buffer)
      throws IOException, InterruptedException, ProtocolViolationException {
    SessionAttributes sessionAttributes = new SessionAttributes();
    getNegotiationResponseSessionAttributes(buffer, sessionAttributes);
    if (sessionAttributes.getTimestamp() != lastRequestTimestamp) {
      throw new ProtocolViolationException(
          String.format("NegotiationResponse timestamp %d does not match last request timestamp %d",
              sessionAttributes.getTimestamp(), lastRequestTimestamp));
    }
    inboundFlowType = sessionAttributes.getFlowType();
    setSessionState(SessionState.NEGOTIATED);
    lastRequestTimestamp = getTimeAsNanos();
    doSendEstablish(sessionId, lastRequestTimestamp, heartbeatInterval, nextSeqNoReceived,
        credentials);
  }

  private void setSessionState(SessionState sessionState) {
    boolean successful = false;
    switch (sessionState) {
      case ESTABLISHED:
        successful =
            this.sessionState.compareAndSet(SessionState.NEGOTIATED, SessionState.ESTABLISHED)
                || this.sessionState.compareAndSet(SessionState.NOT_ESTABLISHED,
                    SessionState.ESTABLISHED);
        break;
      case FINALIZED:
        successful =
            this.sessionState.compareAndSet(SessionState.ESTABLISHED, SessionState.FINALIZED);
        break;
      case NEGOTIATED:
        successful =
            this.sessionState.compareAndSet(SessionState.NOT_NEGOTIATED, SessionState.NEGOTIATED);
        break;
      case NOT_ESTABLISHED:
        successful =
            this.sessionState.compareAndSet(SessionState.ESTABLISHED, SessionState.NOT_ESTABLISHED);
        break;
    }
    if (successful) {
      eventPublisher.submit(new SessionEvent(sessionState, sessionId, principal));
    }
  }

  /**
   * Returns sequence number of received application message Side effect: sequence number is
   * incremented
   * 
   * @param buffer holds an application message
   */
  protected void applicationMessageReceived(ByteBuffer buffer) {
    long seqNo = nextSeqNoReceived;
    nextSeqNoReceived++;
    if (seqNo >= nextSeqNoAccepted) {
      sessionMessageConsumer.accept(principal, buffer, seqNo);
      nextSeqNoAccepted++;
    }
    // else duplicate message ignored
  }

  protected abstract void doDisconnect();

  protected abstract void doSendEstablish(byte[] sessionId, long timestamp, long heartbeatInterval,
      long nextSeqNo, byte[] credentials) throws IOException, InterruptedException;

  protected abstract void doSendEstablishAck(byte[] sessionId, long timestamp,
      long heartbeatInterval, long nextSeqNo) throws IOException, InterruptedException;

  protected abstract void doSendEstablishReject(byte[] sessionId, long timestamp,
      EstablishmentReject rejectCode, byte[] reason) throws IOException, InterruptedException;

  protected abstract void doSendMessage(ByteBuffer buffer) throws IOException, InterruptedException;

  protected abstract void doSendNegotiate(byte[] sessionId, long timestamp, FlowType clientFlow,
      byte[] credentials) throws IOException, InterruptedException;

  protected abstract void doSendNegotiationReject(byte[] sessionId, long requestTimestamp,
      NegotiationReject rejectCode, byte[] reason) throws IOException, InterruptedException;

  protected abstract void doSendNegotiationResponse(byte[] sessionId, long requestTimestamp,
      FlowType serverFlow, byte[] credentials) throws IOException, InterruptedException;

  protected abstract void doSendNotApplied(long fromSeqNo, long count)
      throws IOException, InterruptedException;

  protected abstract void doSendRetransmission(SequenceRange range)
      throws IOException, InterruptedException;

  protected abstract void doSendRetransmitRequest(SequenceRange range)
      throws IOException, InterruptedException;

  protected abstract void doSendSequence(long nextSeqNo) throws IOException, InterruptedException;

  protected abstract void getEstablishmentAckSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes);

  protected abstract void getEstablishSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes);

  protected abstract MessageType getMessageType(ByteBuffer buffer);

  protected abstract void getNegotiateSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes);

  protected abstract void getNegotiationResponseSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes);

  protected abstract long getNextSequenceNumber(ByteBuffer buffer);

  protected abstract void getRetransmissionSequenceRange(ByteBuffer buffer, SequenceRange range);

  protected abstract void getRetransmitRequestSequenceRange(ByteBuffer buffer, SequenceRange range);

  protected abstract boolean isClientSession();

  protected void retransmissionMessageReceived(ByteBuffer buffer)
      throws ProtocolViolationException {
    getRetransmissionSequenceRange(buffer, retransmitRange);
    if (retransmitRange.getTimestamp() != lastRequestTimestamp) {
      throw new ProtocolViolationException(
          String.format("Retransmission timestamp %d does not match last request timestamp %d",
              retransmitRange.getTimestamp(), lastRequestTimestamp));
    }
    nextRetransSeqNoReceived = retransmitRange.getFromSeqNo();
    lastRetransSeqNoToAccept = retransmitRange.getFromSeqNo() + retransmitRange.getCount();
    isReceivingRetransmission = true;
  }

  protected void retransmitAsync(SequenceRange retransmitRange) {
    Objects.requireNonNull(retransmitRange);

    executor.execute(() -> {
      List<ByteBuffer> buffers = sendCache.subList((int) retransmitRange.getFromSeqNo(),
          (int) (retransmitRange.getFromSeqNo() + retransmitRange.getCount()));
      try {
        while (!sendCriticalSection.compareAndSet(false, true)) {
          Thread.yield();
        }
        isSendingRetransmission = true;
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

  protected void retransmittedApplicationMessageReceived(ByteBuffer buffer) {
    long seqNo = nextRetransSeqNoReceived;
    nextRetransSeqNoReceived++;
    if (seqNo < lastRetransSeqNoToAccept) {
      sessionMessageConsumer.accept(principal, buffer, seqNo);
    }
  }

  protected void scheduleHeartbeats() {
    // Give 10% margin for receiving
    heartbeatDueTask = new HeartbeatDueTask();
    timer.scheduleAtFixedRate(heartbeatDueTask, (heartbeatDueInterval * 11) / 10,
        heartbeatDueInterval);
    heartbeatSendTask = new HeartbeatSendTask();
    timer.scheduleAtFixedRate(heartbeatSendTask, heartbeatInterval, heartbeatInterval);
  }

  protected void sequenceMessageReceived(ByteBuffer buffer) throws ProtocolViolationException {
    isReceivingRetransmission = false;
    long newNextSeqNo = getNextSequenceNumber(buffer);
    long prevNextSeqNo = nextSeqNoReceived;
    nextSeqNoReceived = newNextSeqNo;
    if (newNextSeqNo > nextSeqNoAccepted) {
      try {
        switch (inboundFlowType) {
          case Recoverable:
            lastRequestTimestamp = getTimeAsNanos();
            retransmitRange.timestamp(lastRequestTimestamp).fromSeqNo(prevNextSeqNo)
                .count(newNextSeqNo - prevNextSeqNo);
            doSendRetransmitRequest(retransmitRange);
            break;
          case Idempotent:
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
    // if seqNo is too low, just ignore messages until high water mark is reached
  }
}
