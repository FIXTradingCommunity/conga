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

package io.fixprotocol.conga.client.session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.fixprotocol.conga.client.io.ClientEndpoint;
import io.fixprotocol.conga.session.sbe.SbeSession;

/**
 * @author Don Mendelson
 *
 */
public class ClientSession extends SbeSession {

  private ClientEndpoint transport;
  
  public static class Builder extends SbeSession.Builder<ClientSession, Builder> {

    @Override
    public ClientSession build() {
      return new ClientSession(this);
    }
    

  }
  
  public static Builder builder() {
    return new Builder();
  }
  
  private ClientSession(Builder builder)  {
    super(builder);
  }

  @Override
  public boolean connected(Object transport, String principal) {
    if (!(transport instanceof ClientEndpoint)) {
      throw new IllegalArgumentException("Unknown transport type");
    }
    this.transport = (ClientEndpoint) transport;
    return super.connected(transport, principal);
  }

  @Override
  protected void doDisconnect() {
    try {
      transport.close();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  protected void sendMessage(ByteBuffer buffer) throws IOException, InterruptedException {
    Objects.requireNonNull(buffer);
    try {
      transport.send(buffer).get();
    } catch (InterruptedException | IllegalStateException | IOException e) {
      throw e;
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IllegalStateException) {
        // Close sent
        throw new IOException(cause);
      } else if (cause instanceof IOException) {
        throw (IOException) cause;
      } else {
        throw new RuntimeException(cause);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  protected CompletableFuture<ByteBuffer> sendMessageAsync(ByteBuffer buffer) {
    Objects.requireNonNull(buffer);
    try {
      return transport.send(buffer);
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  @Override
  protected boolean isClientSession() {
    return true;
  }
}
