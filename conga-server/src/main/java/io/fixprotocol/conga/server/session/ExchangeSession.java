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

import io.fixprotocol.conga.server.io.BinaryExchangeSocket;
import io.fixprotocol.conga.session.Session;

/**
 * @author Don Mendelson
 *
 */
public class ExchangeSession extends Session {

  private BinaryExchangeSocket transport;
  
  @Override
  public boolean connected(Object transport) {
    final boolean connected = super.connected(transport);
    if (connected) {
      this.transport = (BinaryExchangeSocket) transport;
    }
    return connected;
  }

  public void disconnect() {
    if (transport !=  null) {
      transport.close();
    }
  }

  public void send(ByteBuffer buffer) {
    try {
      if (transport !=  null) {
        transport.send(buffer);
        messageSent();
      }
    } catch (IOException e) {
      // TODO persist messages for recovery
      e.printStackTrace();
    }
  }

}
