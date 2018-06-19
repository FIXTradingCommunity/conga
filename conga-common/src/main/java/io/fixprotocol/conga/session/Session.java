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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Abstract FIXP session, independent of encoding
 * 
 * @author Don Mendelson
 */
public abstract class Session {

  public enum MessageType {
    APPLICATION,
    IGNORE,
    NOT_APPLIED, 
    SEQUENCE
  }
  private final AtomicBoolean connected = new AtomicBoolean();
  private final AtomicLong nextSeqNoAccepted = new AtomicLong(1);
  private final AtomicLong nextSeqNoReceived = new AtomicLong(1L);  

  private final AtomicLong nextSeqNoSent = new AtomicLong(1L);

  /**
   * Returns sequence number of received application message
   * Side effect: sequence number is incremented
   * @return sequence number
   */
  public long applicationMessageReceived() {
    return nextSeqNoReceived.getAndIncrement();
  }

  /**
   * Returns sequence number of sent application message
   * Side effect: sequence number is incremented
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
    return connected.compareAndSet(false, true);
  }

  /**
   * The underlying transport was disconnected
   * 
   * @return Returns {@code true} if the transport was previously connected, {@code false} if it was
   *         already disconnected.
   */

  public boolean disconnected() {
    return connected.compareAndSet(true, false);
  }

  public long getNextSeqNoReceived() {
    return nextSeqNoReceived.get();
  }

  public long getNextSeqNoSent() {
    return nextSeqNoSent.get();
  }

  public boolean isConnected() {
    return connected.get();
  }

  /**
   * @param buffer
   */
  public MessageType messageReceived(ByteBuffer buffer) {
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
        seqNo =  applicationMessageSent();
      } else {
        throw new IllegalStateException("Not connected");
      }
    } catch (IOException e) {
      disconnected();
      throw e;
    }
    return seqNo;
  }

  public void sendHeartbeat() {
    try {
      doSendHeartbeat(getNextSeqNoSent());
    } catch (IOException | InterruptedException e) {
      disconnected();
    }
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
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  protected abstract void doSendHeartbeat(long nextSeqNo) throws IOException, InterruptedException;
  protected abstract void doSendMessage(ByteBuffer buffer) throws IOException, InterruptedException;
  protected abstract void doSendNotApplied(long fromSeqNo, long count) throws IOException, InterruptedException;
  protected abstract MessageType getMessageType(ByteBuffer buffer);
  protected abstract long getNextSequenceNumber(ByteBuffer buffer);

}
