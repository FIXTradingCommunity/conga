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

package io.fixprotocol.conga.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.fixprotocol.conga.buffer.BufferPool;
import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.RingBufferSupplier;
import io.fixprotocol.conga.client.io.ClientEndpoint;
import io.fixprotocol.conga.client.session.ClientSession;
import io.fixprotocol.conga.messages.ApplicationMessageConsumer;
import io.fixprotocol.conga.messages.Message;
import io.fixprotocol.conga.messages.MessageException;
import io.fixprotocol.conga.messages.MutableMessage;
import io.fixprotocol.conga.messages.MutableNewOrderSingle;
import io.fixprotocol.conga.messages.MutableOrderCancelRequest;
import io.fixprotocol.conga.messages.MutableRequestMessageFactory;
import io.fixprotocol.conga.messages.ResponseMessageFactory;
import io.fixprotocol.conga.messages.sbe.SbeMutableRequestMessageFactory;
import io.fixprotocol.conga.messages.sbe.SbeResponseMessageFactory;
import io.fixprotocol.conga.session.SessionMessageConsumer;
import io.fixprotocol.conga.session.FlowType;
import io.fixprotocol.conga.session.ProtocolViolationException;
import io.fixprotocol.conga.session.Session;

/**
 * Trader application sends orders and cancels to Exchange and receives executions
 * <p>
 * Assumption: Trader has 1:1 relationship with session and transport instances.
 * <p>
 * Session/transport layer: WebSocket client over TLS
 * <p>
 * Presentation layer: Simple Binary Encoding (SBE)
 * @author Don Mendelson
 *
 */
public class Trader implements AutoCloseable {

  /**
   * Builds an instance of {@code Trader}
   * <p>
   * Example:
   * 
   * <pre>
   * Trader trader =
   *     Trader.builder().host("1.2.3.4").port("567").build();
   * </pre>
   *
   */
  public static class Builder {
    
    private static final String WEBSOCKET_SCHEME = "wss";
    
    private static URI createUri(String host, int port, String path) throws URISyntaxException {
      return new URI(WEBSOCKET_SCHEME, null, host, port, path, null, null);
    }
    
    private Consumer<Throwable> errorListener = null;
    private String host = DEFAULT_HOST;
    private ApplicationMessageConsumer messageListener = null;
    private String path = DEFAULT_PATH;
    private int port = DEFAULT_PORT;
    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    private URI uri = null;

    protected Builder() {

    }

    public Trader build() throws URISyntaxException {
      if (null == this.uri) {
        this.uri = createUri(host, port, path);
      }

      return new Trader(this);
    }

    public Builder errorListener(Consumer<Throwable> errorListener) {
      this.errorListener = errorListener;
      return this;
    }

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder messageListener(ApplicationMessageConsumer messageListener) {
      this.messageListener = messageListener;
      return this;
    }

    public Builder path(String path) {
      this.path = path;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }
    
    public Builder timeoutSeconds(int timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
      return this;
    }

    public Builder uri(URI uri) {
      this.uri = uri;
      return this;
    }

  }
  public static final String DEFAULT_HOST = "localhost";
  public static final String DEFAULT_PATH = "/trade";
  public static final int DEFAULT_PORT = 443;  
  public static final int DEFAULT_TIMEOUT_SECONDS = 30;
  
  public static Builder builder() {
    return new Builder();
  }
  
  private final BufferSupplier bufferSupplier = new BufferPool();
  private final ClientEndpoint endpoint;
  private final Consumer<Throwable> errorListener;
  private ApplicationMessageConsumer messageListener = null;
  private final MutableRequestMessageFactory requestFactory =
      new SbeMutableRequestMessageFactory(bufferSupplier); 
  private final ResponseMessageFactory responseFactory = new SbeResponseMessageFactory();
  private final RingBufferSupplier ringBuffer;
  private ClientSession session;
  
  // Consumes messages from ring buffer
  private final BiConsumer<String, ByteBuffer> incomingMessageConsumer = (source, buffer) -> {
    try {
      session.messageReceived(buffer);
    } catch (ProtocolViolationException | MessageException | IOException | InterruptedException e) {
      if (getErrorListener() != null) {
        getErrorListener().accept(e);
      }
     session.disconnected();
    }
  };
  // Consumes application messages from Session
  private SessionMessageConsumer sessionMessageConsumer = (source, buffer, seqNo) -> {
    Message message;
    try {
      message = responseFactory.wrap(buffer);
      messageListener.accept(source, message, seqNo);
    } catch (MessageException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  };
  
  private final int timeoutSeconds; 
  private final Timer timer = new Timer("Client-timer", true);

  private Trader(Builder builder) {
    this.messageListener = Objects.requireNonNull(builder.messageListener, "Message listener not set");
    this.errorListener = builder.errorListener;
    this.timeoutSeconds = builder.timeoutSeconds;
    this.ringBuffer = new RingBufferSupplier(incomingMessageConsumer);
    this.endpoint = new ClientEndpoint(ringBuffer, builder.uri, builder.timeoutSeconds);
  }
  
  public void close() {
    try {
      endpoint.close();
      if (session != null) {
        session.disconnected();
      }
      ringBuffer.stop();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Returns an order message encoder
   * 
   * The encoder is thread-safe. That is, messages may be created on multiple threads concurrently.
   * However, a message must be populated on the same thread that invoked the create method.
   * 
   * @return a mutable order message
   */
  public MutableNewOrderSingle createOrder() {
    return requestFactory.getNewOrderSingle();
  }

  /**
   * Returns an order cancel message encoder
   * 
   * The encoder is thread-safe. That is, messages may be created on multiple threads concurrently.
   * However, a message must be populated on the same thread that invoked the create method.
   * 
   * @return a mutable order cancel message
   */
  public MutableOrderCancelRequest createOrderCancelRequest() {
    return requestFactory.getOrderCancelRequest();
  }

  public void open() throws Exception {
    ringBuffer.start();
    if (session == null) {
      UUID uuid = UUID.randomUUID();
      this.session = ClientSession.builder()
          .sessionId(Session.UUIDAsBytes(uuid))
          .timer(timer)
          .heartbeatInterval(TimeUnit.SECONDS.toMillis(timeoutSeconds))
          .sessionMessageConsumer(sessionMessageConsumer)
          .inboundFlowType(FlowType.RECOVERABLE)
          .build();
    }
    endpoint.open();
    session.connected(endpoint, endpoint.getSource());
  }

  /**
   * Send an order or cancel request
   * 
   * @param message
   * @return sequence number of the sent message
   * @throws TimeoutException if the operation fails to complete in a timeout period
   * @throws InterruptedException if the current thread is interrupted
   * @throws IOException if an I/O error occurs
   */
  public long send(MutableMessage message) throws IOException, InterruptedException {
    Objects.requireNonNull(message);
    try {
      return session.sendApplicationMessage(message.toBuffer());
    } finally {
      message.release();
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Trader [session=").append(session).append(", endpoint=").append(endpoint)
        .append(", timeoutSeconds=").append(timeoutSeconds).append("]");
    return builder.toString();
  }

  Consumer<Throwable> getErrorListener() {
    return errorListener;
  }

}
