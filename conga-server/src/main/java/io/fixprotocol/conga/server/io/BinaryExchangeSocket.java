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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import io.fixprotocol.conga.buffer.BufferSupplier.BufferSupply;
import io.fixprotocol.conga.buffer.RingBufferSupplier;

/**
 * Binary WebSocket to communicate with an exchange client
 * 
 * Received data in enqueued in a ring buffer for asynchronous processing. Outbound data is sent
 * synchronously.
 * <p>
 * The principal of a request must be retained with a message in order to route responses back to
 * the source.
 * 
 * @author Don Mendelson
 *
 */
@WebSocket
public class BinaryExchangeSocket {

  private static final Map<String, Session> sessions = new HashMap<>();
  private final String principal;
  private final RingBufferSupplier ringBuffer;

  /**
   * @param principal
   * 
   */
  public BinaryExchangeSocket(RingBufferSupplier ringBuffer, String principal) {
    this.ringBuffer = ringBuffer;
    this.principal = principal;
  }

  @OnWebSocketClose
  public void onClose(Session session, int statusCode, String reason) {
    sessions.remove(principal);
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
    // don't replace an existing session; could be a problem if session does not close properly
    sessions.putIfAbsent(principal, session);
  }

  public static void send(String principal, ByteBuffer buffer) throws IOException {
    Session session = sessions.get(principal);
    if (null != session) {
      session.getRemote().sendBytes(buffer);
    }
  }
}
