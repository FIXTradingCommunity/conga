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

package io.fixprotocol.conga.client.io;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import io.fixprotocol.conga.buffer.BufferSupplier.BufferSupply;
import io.fixprotocol.conga.buffer.RingBufferSupplier;
import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpTimeoutException;
import jdk.incubator.http.WebSocket;
import jdk.incubator.http.WebSocket.MessagePart;
import jdk.incubator.http.WebSocketHandshakeException;

/**
 * WebSocket client endpoint
 * 
 * @author Don Mendelson
 *
 */
public class ClientEndpoint implements AutoCloseable {

  private static final String WEBSOCKET_SCHEME = "wss";

  private static void throwCause(ExecutionException e) throws Exception {
    Throwable cause = e.getCause();
    if (e instanceof Exception) {
      throw (Exception) cause;
    } else {
      throw new RuntimeException(cause);
    }
  }

  private final String host;
  private final ReentrantLock lock = new ReentrantLock();
  private boolean isOpen = false;

  /**
   * 
   * WebSocket implementation automatically responds to ping and close frames. This implementation
   * only handles binary application messages.
   */
  private final WebSocket.Listener listener = new WebSocket.Listener() {
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer src, MessagePart part) {
      webSocket.request(1);

      BufferSupply bufferSupply = ringBuffer.get();
      bufferSupply.acquireAndCopy(src);
      bufferSupply.release();

      // Returning null indicates normal completion
      return null;
    }

    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      // WebSocket implementation automatically sends close response
      lock.lock();
      try {
        isOpen = false;
        ClientEndpoint.this.webSocket = null;
        return null;
      } finally {
        lock.unlock();
      }
    }

    public void onError(WebSocket webSocket, Throwable error) {
      // the session is already torn down; clean up the reference
      lock.lock();
      try {
        isOpen = false;
        error.printStackTrace();
      } finally {
        lock.unlock();
      }
    }

    public void onOpen(WebSocket webSocket) {
      webSocket.request(1);
    }

    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
      webSocket.request(1);
      return null;
    }
  };
  private String path;

  private int port;
  private RingBufferSupplier ringBuffer;
  private long timeoutSeconds;
  private WebSocket webSocket = null;

  /**
   * Construct a WebSocket client endpoint
   * 
   * @param ringBuffer
   * @param host
   * @param port
   * @param path
   * @param timeoutSeconds2
   */
  public ClientEndpoint(RingBufferSupplier ringBuffer, String host, int port, String path,
      int timeoutSeconds) {
    this.ringBuffer = ringBuffer;
    this.timeoutSeconds = timeoutSeconds;
    this.host = host;
    this.port = port;
    this.path = path;
  }

  @Override
  public void close() throws Exception {
    lock.lock();
    try {
      isOpen = false;
      webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "").get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      throwCause(e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Opens a WebSocket to the server
   * 
   * @throws InterruptedException
   * 
   * @throws Throwable complete exceptionally with one of the following errors:
   *         <ul>
   *         <li>{@link IOException} - if an I/O error occurs
   *         <li>{@link WebSocketHandshakeException} - if the opening handshake fails
   *         <li>{@link HttpTimeoutException} - if the opening handshake does not complete within
   *         the timeout
   *         <li>{@link InterruptedException} - if the operation is interrupted
   *         <li>{@link SecurityException} - if a security manager has been installed and it denies
   *         {@link java.net.URLPermission access} to {@code uri}.
   *         <a href="HttpRequest.html#securitychecks">Security checks</a> contains more information
   *         relating to the security context in which the the listener is invoked.
   *         <li>{@link IllegalArgumentException} - if any of the arguments to the constructor are
   *         invalid
   *         </ul>
   * 
   */
  public void open() throws Exception {
    lock.lock();
    try {
      if (isOpen) {
        return;
      }
      URI uri = new URI(WEBSOCKET_SCHEME, null, host, port, path, null, null);
      webSocket = HttpClient.newHttpClient().newWebSocketBuilder().subprotocols("binary")
          .connectTimeout(Duration.ofSeconds(timeoutSeconds)).buildAsync(uri, listener).get();
      isOpen = true;
    } catch (ExecutionException e) {
      throwCause(e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Sends a buffer containing a complete message to the server
   * 
   * @param data The message consists of bytes from the buffer's position to its limit. Upon normal
   *        completion the buffer will have no remaining bytes.
   * @throws TimeoutException if the operation fails to complete in a timeout period
   * @throws InterruptedException if the current thread is interrupted
   * @throws IOException if an I/O error occurs or the WebSocket is not open
   */
  public void send(ByteBuffer data) throws Exception {
    lock.lock();
    try {
      if (!isOpen) {
        throw new IOException("Socket not open");
      }
      webSocket.sendBinary(data, true).get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      throwCause(e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * 
   * @throws InterruptedException
   * @throws TimeoutException
   * @throws IOException
   */
  void ping() throws Exception {
    lock.lock();
    try {
      if (!isOpen) {
        throw new IOException("Socket not open");
      }
      ByteBuffer message = null;
      webSocket.sendPing(message).get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      throwCause(e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * 
   * @throws InterruptedException
   * @throws TimeoutException
   * @throws IOException
   */
  void pong() throws Exception {
    lock.lock();
    try {
      if (!isOpen) {
        throw new IOException("Socket not open");
      }
      ByteBuffer message = null;
      webSocket.sendPong(message).get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      throwCause(e);
    } finally {
      lock.unlock();
    }
  }

}
