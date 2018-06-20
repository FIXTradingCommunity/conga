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

package io.fixprotocol.conga.server.session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;

import io.fixprotocol.conga.server.io.BinaryExchangeSocket;
import io.fixprotocol.conga.session.sbe.SbeSession;

/**
 * Server FIXP session
 * 
 * @author Don Mendelson
 *
 */
public class ServerSession extends SbeSession {

  private BinaryExchangeSocket transport;

  public ServerSession(Timer timer, long heartbeatInterval) {
    super(timer, heartbeatInterval);
  }

  @Override
  public boolean connected(Object transport) {
    if (!(transport instanceof BinaryExchangeSocket)) {
      throw new IllegalArgumentException("Unknown transport type");
    }
    final boolean connected = super.connected(transport);
    if (connected) {
      this.transport = (BinaryExchangeSocket) transport;
    }
    return connected;
  }

  public void disconnect() {
    if (null != transport) {
      transport.close();
    }
  }

  @Override
  protected void doDisconnect() {
    transport.close();
  }

  @Override
  protected void doSendMessage(ByteBuffer buffer) throws IOException {
    transport.send(buffer);
  }

}
