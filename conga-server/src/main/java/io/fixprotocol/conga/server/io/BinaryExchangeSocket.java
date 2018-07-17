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

package io.fixprotocol.conga.server.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import io.fixprotocol.conga.buffer.BufferSupplier.BufferSupply;
import io.fixprotocol.conga.buffer.RingBufferSupplier;
import io.fixprotocol.conga.server.session.ServerSessions;

/**
 * Binary WebSocket to communicate with an exchange client
 * 
 * Received data in enqueued in a ring buffer for asynchronous processing. Outbound data is sent
 * synchronously.
 * <p>
 * The principal of a request must be retained with a message in order to route responses back to
 * the source.
 * <p>
 * Note that {@code Session} class refers to a WebSocket transport connection, not to a Conga/FIXP session.
 * 
 * @author Don Mendelson
 *
 */
@WebSocket
public class BinaryExchangeSocket {

  private final String principal;
  private final RingBufferSupplier ringBuffer;
  private Session webSocketSession;
  private final io.fixprotocol.conga.session.Session fixSession;

  /**
   * @param principal
   * 
   */
  public BinaryExchangeSocket(ServerSessions sessions, RingBufferSupplier ringBuffer, String principal) {
    this.ringBuffer = ringBuffer;
    this.principal = principal;
    this.fixSession = sessions.getSession(principal);
  }

  @OnWebSocketClose
  public void onClose(Session session, int statusCode, String reason) {
    fixSession.disconnected();
  }

  @OnWebSocketError
  public void OnError(Session session, Throwable error) {
    // todo log errors
    error.printStackTrace();
  }

  @OnWebSocketMessage
  public void onMessage(Session session, byte src[], int offset, int length) {
    BufferSupply supply = ringBuffer.get();
    if (null != supply.acquireAndCopy(src, offset, length)) {
      supply.setSource(principal);
      supply.release();
    } else {
      // todo log error
    }
  }

  @OnWebSocketConnect
  public void onOpen(Session session) {
    this.webSocketSession = session;
    this.fixSession.connected(this, principal);
  }

  public final void send(ByteBuffer buffer) throws IOException {
    // synchronous send
    webSocketSession.getRemote().sendBytes(buffer);
  }
  
  public final void close() {
    // code for normal closure
    webSocketSession.close(1000, "");
  }
}
