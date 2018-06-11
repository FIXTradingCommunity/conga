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
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

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

  /**
   * Create a WebSocket URI
   * 
   * @param host remote host
   * @param port remote port
   * @param path URI path
   * @return a URI
   * @throws URISyntaxException if a URI syntax error occurs
   */
  public static URI createUri(String host, int port, String path) throws URISyntaxException {
    return new URI(WEBSOCKET_SCHEME, null, host, port, path, null, null);
  }

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
      webSocket.request(1);
      ClientEndpoint.this.webSocketRef.set(null);
      return null;
    }

    public void onError(WebSocket webSocket, Throwable error) {
      // the session is already torn down; clean up the reference
      webSocket.request(1);
      error.printStackTrace();
      ClientEndpoint.this.webSocketRef.set(null);
    }

    public void onOpen(WebSocket webSocket) {
      webSocket.request(1);
    }

    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
      webSocket.request(1);
      return null;
    }
  };

  private ByteBuffer pong = ByteBuffer.allocateDirect(1024);
  private RingBufferSupplier ringBuffer;
  private long timeoutSeconds;
  private final URI uri;
  private AtomicReference<WebSocket> webSocketRef = new AtomicReference<>();

  /**
   * Construct a WebSocket client endpoint
   * 
   * @param ringBuffer buffer to queue incoming events
   * @param uri WebSocket URI of the remote server
   * @param timeoutSeconds timeout of open and send operations
   */
  public ClientEndpoint(RingBufferSupplier ringBuffer, URI uri, int timeoutSeconds)
      throws URISyntaxException {
    this.ringBuffer = ringBuffer;
    this.uri = uri;
    this.timeoutSeconds = timeoutSeconds;
  }

  @Override
  public void close() throws Exception {
    final WebSocket webSocket = webSocketRef.get();
    if (webSocket != null) {
      webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "").get(timeoutSeconds, TimeUnit.SECONDS);
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
    webSocketRef.compareAndSet(null,
        HttpClient.newHttpClient().newWebSocketBuilder().subprotocols("binary")
            .connectTimeout(Duration.ofSeconds(timeoutSeconds)).buildAsync(uri, listener).get());
  }

  /**
   * Sends a buffer containing a complete message to the server
   * 
   * @param data The message consists of bytes from the buffer's position to its limit. Upon normal
   *        completion the buffer will have no remaining bytes.
   * @return
   * @throws TimeoutException if the operation fails to complete in a timeout period
   * @throws ExecutionException if other exceptions occurred
   * @throws InterruptedException if the current thread is interrupted
   * @throws IOException if an I/O error occurs or the WebSocket is not open
   */
  public CompletableFuture<ByteBuffer> send(ByteBuffer data) throws Exception {
    final WebSocket webSocket = webSocketRef.get();
    if (webSocket != null) {
      CompletableFuture<WebSocket> future = webSocket.sendBinary(data, true);
      return future.thenCompose(w -> {
        return CompletableFuture.completedFuture(data);
      });
    } else {
      throw new IOException("WebSocket not open");
    }
  }



  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("ClientEndpoint [timeoutSeconds=").append(timeoutSeconds).append(", uri=")
        .append(uri).append(", webSocket=").append(webSocketRef.get()).append("]");
    return builder.toString();
  }

  /**
   * Send WebSocket pong message
   * 
   * @throws TimeoutException if the operation fails to complete in a timeout period
   * @throws ExecutionException if other exceptions occurred
   * @throws InterruptedException if the current thread is interrupted
   * @throws IOException if an I/O error occurs or the WebSocket is not open
   */
  void pong() throws Exception {
    final WebSocket webSocket = webSocketRef.get();
    if (webSocket != null) {
      webSocket.sendPong(pong).get(timeoutSeconds, TimeUnit.SECONDS);
    } else {
      throw new IOException("WebSocket not open");
    }

  }


  void setPong(byte[] src) {
    pong.put(src);
    pong.flip();
  }

}
