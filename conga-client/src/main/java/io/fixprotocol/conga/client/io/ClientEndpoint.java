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
import java.security.Principal;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;

import io.fixprotocol.conga.buffer.BufferSupplier.BufferSupply;
import io.fixprotocol.conga.buffer.RingBufferSupplier;

import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;

/**
 * WebSocket client endpoint
 * 
 * @author Don Mendelson
 *
 */
public class ClientEndpoint implements AutoCloseable {

  private final AtomicBoolean connectedCriticalSection = new AtomicBoolean();

  /**
   * 
   * WebSocket implementation automatically responds to ping and close frames. 
   */
  private final WebSocket.Listener listener = new WebSocket.Listener() {

    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer src, boolean last) {

      if (src.hasRemaining()) {
        BufferSupply bufferSupply = ringBuffer.get();
        bufferSupply.acquireAndCopy(src);
        bufferSupply.release();
      }

      webSocket.request(1);
      // Returning null indicates normal completion
      return null;
    }
    
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence message, boolean last) {
      if (message.length() > 0) {
        BufferSupply bufferSupply = ringBuffer.get();
        ByteBuffer buffer = bufferSupply.acquire();
        String str = message.toString();
        byte [] src = str.getBytes();
        buffer.put(src);
        bufferSupply.release();
      }
      webSocket.request(1);
      return null;
    }

    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      // WebSocket implementation automatically sends close response
      while (!connectedCriticalSection.compareAndSet(false, true)) {
        Thread.yield();
      }
      try {
        ClientEndpoint.this.webSocket = null;
        return null;
      } finally {
        connectedCriticalSection.compareAndSet(true, false);
      }
    }

    public void onError(WebSocket webSocket, Throwable error) {
      // the session is already torn down; clean up the reference
      while (!connectedCriticalSection.compareAndSet(false, true)) {
        Thread.yield();
      }
      try {
        ClientEndpoint.this.webSocket = null;
      } finally {
        connectedCriticalSection.compareAndSet(true, false);
      }
    }

    public void onOpen(WebSocket webSocket) {
      webSocket.request(1);
    }

  };

  private final RingBufferSupplier ringBuffer;
  private String source;
  private final String subprotocol;
  private final long timeoutSeconds;
  private final URI uri;
  private WebSocket webSocket = null;

  /**
   * Construct a WebSocket client endpoint
   * 
   * @param ringBuffer buffer to queue incoming events
   * @param uri WebSocket URI of the remote server
   * @param subprotocol WebSocket subprotocol
   * @param timeoutSeconds timeout of open and send operations
   */
  public ClientEndpoint(RingBufferSupplier ringBuffer, URI uri, String subprotocol,
      int timeoutSeconds) {
    this.ringBuffer = ringBuffer;
    this.uri = uri;
    this.timeoutSeconds = timeoutSeconds;
    this.subprotocol = subprotocol;
  }

  @Override
  public void close() throws Exception {
    while (!connectedCriticalSection.compareAndSet(false, true)) {
      Thread.yield();
    }
    try {
      if (null != webSocket) {
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "").get(timeoutSeconds, TimeUnit.SECONDS);
      }
    } finally {
      connectedCriticalSection.compareAndSet(true, false);
    }
  }

  public String getSource() {
    return source;
  }

  /**
   * Opens a WebSocket to the server
   *
   * @throws Exception complete exceptionally with one of the following errors:
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
    while (!connectedCriticalSection.compareAndSet(false, true)) {
      Thread.yield();
    }
    try {
      if (webSocket == null) {
        final HttpClient httpClient = HttpClient.newHttpClient();
        webSocket = httpClient.newWebSocketBuilder().subprotocols(subprotocol)
            .connectTimeout(Duration.ofSeconds(timeoutSeconds)).buildAsync(uri, listener).get();
        final SSLSessionContext clientSessionContext =
            httpClient.sslContext().getClientSessionContext();
        byte[] id = clientSessionContext.getIds().nextElement();
        SSLSession sslSession = clientSessionContext.getSession(id);
        Principal principal = sslSession.getLocalPrincipal();
        source = (null != principal) ? principal.getName() : new String(id);
      }
    } finally {
      connectedCriticalSection.compareAndSet(true, false);
    }
  }

  /**
   * Sends a buffer containing a complete message to the server asynchronously
   * 
   * @param data The message consists of bytes from the buffer's position to its limit. Upon normal
   *        completion the buffer will have no remaining bytes.
   * @return if successful, returns a future containing the buffer that was sent upon completion
   * @throws TimeoutException if the operation fails to complete in a timeout period
   * @throws ExecutionException if other exceptions occurred
   * @throws InterruptedException if the current thread is interrupted
   * @throws IOException if an I/O error occurs or the WebSocket is not open
   */
  public CompletableFuture<ByteBuffer> send(ByteBuffer data) throws Exception {
    CompletableFuture<WebSocket> future;
    if (null != webSocket) {
      if (subprotocol.equals("text")) {
        int size = data.remaining();
        byte[] dst = new byte[size];
        data.get(dst, 0, size);
        String str = new String(dst);
        future = webSocket.sendText(str, true);
      } else {
        future = webSocket.sendBinary(data, true);
      }
      return future.thenCompose(w -> CompletableFuture.completedFuture(data));
    } else {
      throw new IOException("WebSocket not open");
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("ClientEndpoint [source=").append(source).append(", timeoutSeconds=")
        .append(timeoutSeconds).append(", uri=").append(uri).append(", webSocket=")
        .append(webSocket).append("]");
    return builder.toString();
  }

}
