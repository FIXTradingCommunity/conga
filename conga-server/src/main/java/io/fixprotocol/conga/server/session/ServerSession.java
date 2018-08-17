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
import java.util.concurrent.CompletableFuture;

import io.fixprotocol.conga.server.io.callback.BinaryExchangeSocket;
import io.fixprotocol.conga.session.Session;

/**
 * Server FIXP session
 * 
 * @author Don Mendelson
 *
 */
public class ServerSession extends Session {

  public static class Builder extends Session.Builder<ServerSession, Builder> {

    @Override
    public ServerSession build() {
      return new ServerSession(this);
    }

  }

  public static Builder builder() {
    return new Builder();
  }

  private BinaryExchangeSocket transport;

  private ServerSession(Builder builder) {
    super(builder);
  }

  @Override
  public boolean connected(Object transport, String principal) {
    if (!(transport instanceof BinaryExchangeSocket)) {
      throw new IllegalArgumentException("Unknown transport type");
    }
    final boolean connected = super.connected(transport, principal);
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
  protected boolean isClientSession() {
    return false;
  }

  @Override
  protected void sendMessage(ByteBuffer buffer) throws IOException {
    transport.send(buffer);
  }

  @Override
  protected CompletableFuture<ByteBuffer> sendMessageAsync(ByteBuffer buffer) {
    try {
      transport.sendAsync(buffer).get();
      return CompletableFuture.completedFuture(buffer);
    } catch (Throwable t) {
      return CompletableFuture.failedFuture(t);
    }
  }

}
