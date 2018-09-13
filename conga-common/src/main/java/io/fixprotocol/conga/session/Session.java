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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.fixprotocol.conga.messages.appl.MessageException;
import io.fixprotocol.conga.messages.appl.MutableMessage;
import io.fixprotocol.conga.messages.session.SessionMessenger;

/**
 * Abstract FIXP session, independent of encoding
 * <p>
 * Limitation: does not support FIXP multiplexing. In other words, there is a 1:1 relationship
 * between Session and transport.
 * 
 * @author Don Mendelson
 */
public abstract class Session {

  @SuppressWarnings("unchecked")
  public abstract static class Builder<T extends Session, B extends Builder<T, B>> {

    private byte[] credentials;
    private long lastSeqNoReceived = 0;
    private Executor executor;
    private long heartbeatInterval;
    private FlowType outboundFlowType = FlowType.Idempotent;
    private List<ByteBuffer> sendCache;
    private byte[] sessionId = new byte[16];
    private SessionMessageConsumer sessionMessageConsumer = null;
    private SessionMessenger sessionMessenger;
    private Timer timer;

    protected Builder() {

    }

    public abstract T build();

    /**
     * Credentials for authentication
     * 
     * @param credentials for authentication
     * @return this Builder
     */
    public B credentials(byte[] credentials) {
      Objects.requireNonNull(credentials);
      this.credentials = credentials;
      return (B) this;
    }
    
    /**
     * Executor for asynchronous operations
     * 
     * @param executor executor implementation
     * @return this Builder
     */
    public B executor(Executor executor) {
      Objects.requireNonNull(executor);
      this.executor = executor;
      return (B) this;
    }

    /**
     * Heartbeat interval in millis
     * 
     * @param heartbeatInterval interval in millis
     * @return this Builder
     */
    public B heartbeatInterval(long heartbeatInterval) {
      this.heartbeatInterval = heartbeatInterval;
      return (B) this;
    }

    /**
     * If rebuilding a failed session, set the sequence number of the last appplication message
     * received.
     * 
     * @param lastSeqNoReceived sequence number of last message received
     * @return this Builder
     */
    public B lastSeqNoReceived(long lastSeqNoReceived) {
      this.lastSeqNoReceived = lastSeqNoReceived;
      return (B) this;
    }

    /**
     * Delivery guarantee for outbound messages
     * 
     * @param outboundFlowType outbound flow type
     * @return this Builder
     */
    public B outboundFlowType(FlowType outboundFlowType) {
      Objects.requireNonNull(outboundFlowType);
      this.outboundFlowType = outboundFlowType;
      return (B) this;
    }

    /**
     * An interface to persist outbound messages for recoverability.
     *
     * @param sendCache an interface to persist messages
     * @return this Builder
     */
    public B sendCache(List<ByteBuffer> sendCache) {
      Objects.requireNonNull(sendCache);
      this.sendCache = sendCache;
      return (B) this;
    }

    /**
     * A transient FIXP session identifier
     * 
     * @param sessionId session identifier in the form of a UUID
     * @return this Builder
     */
    public B sessionId(byte[] sessionId) {
      Objects.requireNonNull(sessionId);
      this.sessionId = sessionId;
      return (B) this;
    }

    /**
     * A consumer of messages received through this Session
     * 
     * @param sessionMessageConsumer a message consumer
     * @return this Builder
     */
    public B sessionMessageConsumer(SessionMessageConsumer sessionMessageConsumer) {
      Objects.requireNonNull(sessionMessageConsumer);
      this.sessionMessageConsumer = sessionMessageConsumer;
      return (B) this;
    }

    /**
     * FIXP session message encoder/decoder
     * 
     * @param sessionMessenger session message encoder/decoder
     * @return this Builder
     */
    public B sessionMessenger(SessionMessenger sessionMessenger) {
      Objects.requireNonNull(sessionMessenger);
      this.sessionMessenger = sessionMessenger;
      return (B) this;
    }

    /**
     * A shared Timer for scheduled events
     * 
     * @param timer a shared event timer
     * @return this Builder
     */
    public B timer(Timer timer) {
      Objects.requireNonNull(timer);
      this.timer = timer;
      return (B) this;
    }

  }

  private class HeartbeatDueTask extends TimerTask {

    @Override
    public void run() {
      if (isHeartbeatDueToReceive()) {
        setSessionState(SessionState.NOT_ESTABLISHED);
        terminate();
      }
    }
  }

  private class HeartbeatSendTask extends TimerTask {

    @Override
    public void run() {
      if (isHeartbeatDueToSend()) {
        MutableMessage mutableMessage = null;
        try {        
          switch (getSessionState()) {
            case ESTABLISHED:
              mutableMessage = sessionMessenger.encodeSequence(nextSeqNoSent.get());
              sendMessage(mutableMessage.toBuffer());             
              break;
            case FINALIZE_REQUESTED:
              mutableMessage = 
                  sessionMessenger.encodeFinishedSending(sessionId, nextSeqNoSent.get() - 1);
              sendMessage(mutableMessage.toBuffer());
              break;
          }
        } catch (IOException | InterruptedException | IllegalStateException e) {
          suspend();
        } finally {
          if (mutableMessage != null) {
            mutableMessage.release();
          }
        }
      }
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
  private final byte[] credentials;
  private final SubmissionPublisher<SessionEvent> eventPublisher;
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
  private final FlowType outboundFlowType;
  private String principal;
  private final AtomicBoolean receivedCriticalSection = new AtomicBoolean();
  private final SequenceRange retransmitRange = new SequenceRange();
  private final List<ByteBuffer> sendCache;
  private final AtomicBoolean sendCriticalSection = new AtomicBoolean();
  private byte[] sessionId;
  private final SessionMessageConsumer sessionMessageConsumer;
  private final SessionMessenger sessionMessenger;
  private final AtomicReference<SessionState> sessionState =
      new AtomicReference<>(SessionState.NOT_NEGOTIATED);
  private final Timer timer;

  protected Session(@SuppressWarnings("rawtypes") Builder<? extends Session, ? extends Session.Builder> builder) {
    this.timer = builder.timer;
    this.heartbeatInterval = builder.heartbeatInterval;
    this.sessionId = builder.sessionId;
    this.sessionMessageConsumer = builder.sessionMessageConsumer;
    this.outboundFlowType = builder.outboundFlowType;
    this.sendCache = builder.sendCache;
    this.executor = builder.executor;
    this.credentials = builder.credentials;
    this.nextSeqNoReceived = builder.lastSeqNoReceived + 1;
    this.sessionMessenger = builder.sessionMessenger;
    
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
   * @param transport the transport associated with this Session
   * @param principal party responsible for this Session
   * 
   * @return Returns {@code true} if the transport was previously unconnected, {@code false} if it
   *         was already connected.
   */
  public boolean connected(Object transport, String principal) {
    while (!connectedCriticalSection.compareAndSet(false, true)) {
      Thread.yield();
    }
    MutableMessage mutableMessage = null;
    try {
      if (!isConnected) {
        this.principal = principal;
        this.sessionMessenger.init(isClientSession());
        if (isClientSession()) {
          lastRequestTimestamp = getTimeAsNanos();
          SessionState state = getSessionState();
          
          switch (state) {
            case NOT_NEGOTIATED:
              mutableMessage = sessionMessenger.encodeNegotiate(sessionId, lastRequestTimestamp, outboundFlowType, credentials);
              sendMessage(mutableMessage.toBuffer());
              break;
            case NEGOTIATED:
            case NOT_ESTABLISHED:
              mutableMessage = sessionMessenger.encodeEstablish(sessionId, lastRequestTimestamp, heartbeatInterval, nextSeqNoReceived,
                  credentials);
              sendMessage(mutableMessage.toBuffer());
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
      if (mutableMessage != null) {
        mutableMessage.release();
      }
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
        cancelHeartbeats();
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


  public boolean requestFinalization() throws IOException, InterruptedException {
    final boolean requested = setSessionState(SessionState.FINALIZE_REQUESTED);
    if (requested) {
      MutableMessage mutableMessage = sessionMessenger.encodeFinishedSending(sessionId, nextSeqNoSent.get() - 1);
      try {
        sendMessage(mutableMessage.toBuffer());
      } finally {
        mutableMessage.release();
      }
      return true;
    } else {
      return false;
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
   * @throws Exception if a message cannot be processed
   */
  public void messageReceived(ByteBuffer buffer) throws Exception {
    while (!receivedCriticalSection.compareAndSet(false, true)) {
      Thread.yield();
    }
    try {
      isHeartbeatDueToReceive.set(false);
      SessionMessageType messageType =
          sessionMessenger.getMessageType(buffer);

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
          sessionMessenger.decodeRetransmitRequestSequenceRange(buffer, retransmitRange);
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
        case FINISHED_SENDING:
          MutableMessage mutableMessage = sessionMessenger.encodeFinishedReceiving(sessionId);
          sendMessageAsync(mutableMessage.toBuffer()).thenRun(mutableMessage::release);
          setSessionState(SessionState.FINALIZED);
          terminate();
          break;
        case FINISHED_RECEIVING:
          setSessionState(SessionState.FINALIZED);
          terminate();
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
          MutableMessage mutableMessage = sessionMessenger.encodeSequence(seqNo);
          try {
            sendMessage(mutableMessage.toBuffer());
          } finally {
            mutableMessage.release();
          }
          isSendingRetransmission = false;
        }
        isHeartbeatDueToSend.set(false);
        sendMessage(buffer);

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
    MutableMessage mutableMessage = null;
    try {
      mutableMessage = sessionMessenger.encodeSequence(nextSeqNoSent.get());
      sendMessage(mutableMessage.toBuffer());
    } catch (IOException | InterruptedException e) {
      disconnected();
    } finally {
      if (mutableMessage != null) {
        mutableMessage.release();
      }
    }
  }
  
  public void subscribeForEvents(Subscriber<? super SessionEvent> subscriber) {
    eventPublisher.subscribe(subscriber);
  }
  
  private void terminate() {
    cancelHeartbeats();
    doDisconnect();
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

  private void cancelHeartbeats() {
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
    sessionMessenger.decodeEstablishmentAckSessionAttributes(buffer, sessionAttributes);
    if (sessionAttributes.getTimestamp() != lastRequestTimestamp) {
      throw new ProtocolViolationException(
          String.format("EstablishmentAck timestamp %d does not match last request timestamp %d",
              sessionAttributes.getTimestamp(), lastRequestTimestamp));
    }

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
    sessionMessenger.decodeEstablishSessionAttributes(buffer, sessionAttributes);
    sessionId = sessionAttributes.getSessionId();
    heartbeatDueInterval = sessionAttributes.getKeepAliveInterval();
    long expectedNextSeqNo = sessionAttributes.getNextSeqNo();
    final long timestamp = sessionAttributes.getTimestamp();
    MutableMessage mutableMessage = sessionMessenger.encodeEstablishmentAck(sessionId, timestamp, heartbeatInterval, nextSeqNoReceived);
    try {
      sendMessage(mutableMessage.toBuffer());
    } finally {
      mutableMessage.release();
    }
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
    sessionMessenger.decodeNegotiateSessionAttributes(buffer, sessionAttributes);
    sessionId = sessionAttributes.getSessionId();
    inboundFlowType = sessionAttributes.getFlowType();
    final long timestamp = sessionAttributes.getTimestamp();
    MutableMessage mutableMessage = sessionMessenger.encodeNegotiationResponse(sessionId, timestamp, outboundFlowType, null);
    try {
      sendMessage(mutableMessage.toBuffer());
    } finally {
      mutableMessage.release();
    }
    setSessionState(SessionState.NEGOTIATED);
  }

  private void negotiationRejectMessageReceived(ByteBuffer buffer) {
    setSessionState(SessionState.NOT_NEGOTIATED);
  }

  private void negotiationResponseMessageReceived(ByteBuffer buffer)
      throws IOException, InterruptedException, ProtocolViolationException {
    SessionAttributes sessionAttributes = new SessionAttributes();
    sessionMessenger.decodeNegotiationResponseSessionAttributes(buffer, sessionAttributes);
    if (sessionAttributes.getTimestamp() != lastRequestTimestamp) {
      throw new ProtocolViolationException(
          String.format("NegotiationResponse timestamp %d does not match last request timestamp %d",
              sessionAttributes.getTimestamp(), lastRequestTimestamp));
    }
    inboundFlowType = sessionAttributes.getFlowType();
    setSessionState(SessionState.NEGOTIATED);
    lastRequestTimestamp = getTimeAsNanos();
    MutableMessage mutableMessage = sessionMessenger.encodeEstablish(sessionId, lastRequestTimestamp, heartbeatInterval, nextSeqNoReceived,
        credentials);
    try {
      sendMessage(mutableMessage.toBuffer());
    } finally {
      mutableMessage.release();
    }
  }

  private boolean setSessionState(SessionState sessionState) {
    boolean successful = false;
    switch (sessionState) {
      case ESTABLISHED:
        successful =
            this.sessionState.compareAndSet(SessionState.NEGOTIATED, SessionState.ESTABLISHED)
                || this.sessionState.compareAndSet(SessionState.NOT_ESTABLISHED,
                    SessionState.ESTABLISHED);
        break;
      case FINALIZED:
        successful = this.sessionState.compareAndSet(SessionState.FINALIZE_REQUESTED,
            SessionState.FINALIZED);
        break;
      case NEGOTIATED:
        successful =
            this.sessionState.compareAndSet(SessionState.NOT_NEGOTIATED, SessionState.NEGOTIATED);
        break;
      case NOT_ESTABLISHED:
        successful =
            this.sessionState.compareAndSet(SessionState.ESTABLISHED, SessionState.NOT_ESTABLISHED);
        break;
      case FINALIZE_REQUESTED:
        successful = this.sessionState.compareAndSet(SessionState.ESTABLISHED,
            SessionState.FINALIZE_REQUESTED);
        break;
    }
    if (successful) {
      eventPublisher.submit(new SessionEvent(sessionState, sessionId, principal));
    }
    return successful;
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

  protected abstract boolean isClientSession();


  protected void retransmissionMessageReceived(ByteBuffer buffer)
      throws ProtocolViolationException {
    sessionMessenger.decodeRetransmissionSequenceRange(buffer, retransmitRange);
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
      try {
      List<ByteBuffer> buffers = sendCache.subList((int) retransmitRange.getFromSeqNo(),
          (int) (retransmitRange.getFromSeqNo() + retransmitRange.getCount()));

        while (!sendCriticalSection.compareAndSet(false, true)) {
          Thread.yield();
        }
        isSendingRetransmission = true;
        MutableMessage mutableMessage = sessionMessenger.encodeRetransmission(sessionId, retransmitRange);
        try {
          sendMessage(mutableMessage.toBuffer());
        } finally {
          mutableMessage.release();
        }
        for (ByteBuffer buffer : buffers) {
          sendMessage(buffer);
        }
      } catch(IndexOutOfBoundsException | InterruptedException e) {
        // TODO report error
      } catch (IOException e) {
        disconnected();
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

  /**
   * Send a message synchronously
   * @param buffer holds a message to send
   * @throws IOException if an IO error occurs
   * @throws InterruptedException if the operation is interrupted
   */
  protected abstract void sendMessage(ByteBuffer buffer) throws IOException, InterruptedException;

  /**
   * Send a message asynchronously
   * @param buffer holds a message to send
   * @return a future that provides asynchronous result
   */
  protected abstract CompletableFuture<ByteBuffer> sendMessageAsync(ByteBuffer buffer);

  protected void sequenceMessageReceived(ByteBuffer buffer) throws ProtocolViolationException {
    isReceivingRetransmission = false;
    long newNextSeqNo = sessionMessenger.decodeSequence(buffer);
    long prevNextSeqNo = nextSeqNoReceived;
    nextSeqNoReceived = newNextSeqNo;
    if (newNextSeqNo > nextSeqNoAccepted) {
      MutableMessage mutableMessage = null;
      try {
        
        switch (inboundFlowType) {
          case Recoverable:
            lastRequestTimestamp = getTimeAsNanos();
            retransmitRange.timestamp(lastRequestTimestamp).fromSeqNo(prevNextSeqNo)
                .count(newNextSeqNo - prevNextSeqNo);
            mutableMessage = sessionMessenger.encodeRetransmitRequest(sessionId, retransmitRange);
            sendMessage(mutableMessage.toBuffer());           
            break;
          case Idempotent:
            mutableMessage = sessionMessenger.encodeNotApplied(prevNextSeqNo, newNextSeqNo - prevNextSeqNo);
            sendApplicationMessage(mutableMessage.toBuffer());
            break;
          default:
            // do nothing
            break;
        }
        nextSeqNoAccepted = newNextSeqNo;
      } catch (IOException | InterruptedException e) {
        disconnected();
      } finally {
        if (mutableMessage != null) {
          mutableMessage.release();
      }
      }
    }
    // if seqNo is too low, just ignore messages until high water mark is reached
  }

  public void suspend() {
    setSessionState(SessionState.NOT_ESTABLISHED);
    terminate();
  }
}
