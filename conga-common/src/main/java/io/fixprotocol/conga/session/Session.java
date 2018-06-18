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

  private AtomicBoolean connected = new AtomicBoolean();
  private AtomicLong nextSeqNoReceived = new AtomicLong(1L);
  private AtomicLong nextSeqNoSent = new AtomicLong(1L);  

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

  public long messageReceived() {
    return nextSeqNoReceived.incrementAndGet();
  }

  public long messageSent() {
    return nextSeqNoSent.incrementAndGet();
  }

  /**
   * Send an application message
   * @param buffer buffer containing a message
   * @return the sequence number of the sent message or {@code 0} if an error occurs
   * @throws IOException if an IO error occurs
   * @throws InterruptedException if the operation is interruped before completion
   * @throws IllegalStateException if the transport is not connected
   */
  public long sendApplicationMessage(ByteBuffer buffer) throws IOException, InterruptedException {
    long seqNo = 0;
    try {
      if (isConnected()) {
        doSendMessage(buffer);
        seqNo =  messageSent();
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
  
  protected abstract long doSendHeartbeat(long nextSeqNo) throws IOException, InterruptedException;

  protected abstract void doSendMessage(ByteBuffer buffer) throws IOException, InterruptedException;

}
