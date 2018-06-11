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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Don Mendelson
 */
public class Session {

  private AtomicBoolean connected = new AtomicBoolean();
  private AtomicInteger messagesReceived = new AtomicInteger();
  private AtomicInteger messagesSent = new AtomicInteger();

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

  public int getMessagesReceived() {
    return messagesReceived.get();
  }

  public int getMessagesSent() {
    return messagesSent.get();
  }

  public boolean isConnected() {
    return connected.get();
  }

  public int messageReceived() {
    return messagesReceived.incrementAndGet();
  }

  public int messageSent() {
    return messagesSent.incrementAndGet();
  }


}
