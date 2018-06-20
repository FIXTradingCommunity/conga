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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Abstract FIXP session, independent of encoding
 * <p>
 * Limitation: does not support FIXP multiplexing. In other words, there is a 1:1 relationship
 * between session and transport.
 * 
 * @author Don Mendelson
 */
public abstract class Session {

  public enum MessageType {
    APPLICATION, IGNORE, NOT_APPLIED, SEQUENCE
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

  private final AtomicBoolean criticalSection = new AtomicBoolean();
  private HeartbeatDueTask heartbeatDueTask;;
  private final long heartbeatInterval;
  private HeartbeatSendTask heartbeatSendTask;
  private boolean isConnected = false;
  private final AtomicBoolean isHeartbeatDueToReceive = new AtomicBoolean(true);
  private final AtomicBoolean isHeartbeatDueToSend = new AtomicBoolean(true);
  private final AtomicLong nextSeqNoAccepted = new AtomicLong(1);
  private final AtomicLong nextSeqNoReceived = new AtomicLong(1L);
  private final AtomicLong nextSeqNoSent = new AtomicLong(1L);
  private final Timer timer;


  protected Session(Timer timer, long heartbeatInterval) {
    this.timer = timer;
    this.heartbeatInterval = heartbeatInterval;
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
   * 
   * @return Returns {@code true} if the transport was previously unconnected, {@code false} if it
   *         was already connected.
   */
  public boolean connected(Object transport) {
    while (!criticalSection.compareAndSet(false, true)) {
      Thread.yield();
    }
    try {
      if (!isConnected) {
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

  public long getNextSeqNoReceived() {
    return nextSeqNoReceived.get();
  }

  public long getNextSeqNoSent() {
    return nextSeqNoSent.get();
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
   * @param buffer
   */
  public MessageType messageReceived(ByteBuffer buffer) {
    isHeartbeatDueToReceive.set(false);
    MessageType messageType = getMessageType(buffer);

    long seqNo;
    switch (messageType) {
      case SEQUENCE:
        sequenceMessageReceived(buffer);
        break;
      case APPLICATION:
        seqNo = applicationMessageReceived();
        if (nextSeqNoAccepted.compareAndSet(seqNo, seqNo)) {
          nextSeqNoAccepted.incrementAndGet();
        } else {
          // Duplicate message received
          messageType = MessageType.IGNORE;
        }
        break;
      default:
        break;
    }
    return messageType;
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
        doSendNotApplied(prevNextSeqNo, newNextSeqNo - prevNextSeqNo);
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

  protected abstract MessageType getMessageType(ByteBuffer buffer);

  protected abstract long getNextSequenceNumber(ByteBuffer buffer);

}
