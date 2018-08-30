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
import java.util.ServiceLoader;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.fixprotocol.conga.buffer.BufferPool;
import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.RingBufferSupplier;
import io.fixprotocol.conga.client.io.ClientEndpoint;
import io.fixprotocol.conga.client.session.ClientSession;
import io.fixprotocol.conga.messages.appl.ApplicationMessageConsumer;
import io.fixprotocol.conga.messages.appl.Message;
import io.fixprotocol.conga.messages.appl.MessageException;
import io.fixprotocol.conga.messages.appl.MutableMessage;
import io.fixprotocol.conga.messages.appl.MutableNewOrderSingle;
import io.fixprotocol.conga.messages.appl.MutableOrderCancelRequest;
import io.fixprotocol.conga.messages.appl.MutableRequestMessageFactory;
import io.fixprotocol.conga.messages.appl.ResponseMessageFactory;
import io.fixprotocol.conga.messages.session.SessionMessenger;
import io.fixprotocol.conga.messages.spi.MessageProvider;
import io.fixprotocol.conga.session.Session;
import io.fixprotocol.conga.session.SessionEvent;
import io.fixprotocol.conga.session.SessionMessageConsumer;
import io.fixprotocol.conga.session.SessionState;

/**
 * Trader application sends orders and cancels to Exchange and receives executions
 * <p>
 * Assumption: Trader has 1:1 relationship with session and transport instances.
 * <p>
 * <b>Session/transport layer:</b> WebSocket client over TLS. To configure a trust store for TLS,
 * set these environment variables:
 * 
 * <pre>
 * -Djavax.net.ssl.trustStore=client.pkcs -Djavax.net.ssl.trustStorePassword=storepassword
 * </pre>
 * 
 * <b>Presentation layer:</b> the initial implementation encodes messages using Simple Binary
 * Encoding (SBE)
 * 
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
   * Trader trader = Trader.builder().host("1.2.3.4").port("567").build();
   * </pre>
   *
   */
  public static class Builder<T extends Trader, B extends Builder<T, B>> {


    private static final String WEBSOCKET_SCHEME = "wss";
    public static final String DEFAULT_OUTPUT_PATH = "log";
    public static final String DEFAULT_ENCODING = "SBE";
    public static final long DEFAULT_HEARTBEAT_INTERVAL = 2000L;
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_PATH = "/trade";
    public static final int DEFAULT_PORT = 8025;
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private static URI createUri(String host, int port, String path) throws URISyntaxException {
      return new URI(WEBSOCKET_SCHEME, null, host, port, path, null, null);
    }

    public Subscriber<? super SessionEvent> sessionEventSubscriber;
    private String encoding = DEFAULT_ENCODING;
    private Consumer<Throwable> errorListener = Throwable::printStackTrace;
    private String host = DEFAULT_HOST;
    private ApplicationMessageConsumer messageListener = null;
    private String path = DEFAULT_PATH;
    private int port = DEFAULT_PORT;
    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    private URI uri = null;
    //private String outputPath = DEFAULT_OUTPUT_PATH;
    private long heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;

    protected Builder() {

    }

    @SuppressWarnings("unchecked")
    public B apiPath(String path) {
      this.path = Objects.requireNonNull(path);
      return (B) this;
    }

    @SuppressWarnings("unchecked")
    public T build() throws URISyntaxException {
      if (null == this.uri) {
        this.uri = createUri(host, port, path);
      }

      return (T) new Trader(this);
    }

    @SuppressWarnings("unchecked")
    public B encoding(String encoding) {
      this.encoding = Objects.requireNonNull(encoding);
      return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B errorListener(Consumer<Throwable> errorListener) {
      this.errorListener = Objects.requireNonNull(errorListener);
      return (B) this;
    }
    
    /**
     * Set heartbeat interval 
     * @param heartbeatInterval keepalive interval in millis
     * @return
     */
    @SuppressWarnings("unchecked")
    public B heartbeatInterval(long heartbeatInterval) {
      this.heartbeatInterval = heartbeatInterval;
      return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B messageListener(ApplicationMessageConsumer messageListener) {
      this.messageListener = Objects.requireNonNull(messageListener);
      return (B) this;
    }
    
//    @SuppressWarnings("unchecked")
//    public B outputPath(String path) {
//      this.outputPath = Objects.requireNonNull(path);
//      return (B) this;
//    }

    @SuppressWarnings("unchecked")
    public B remoteHost(String host) {
      this.host = Objects.requireNonNull(host);
      return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B remotePort(int port) {
      this.port = port;
      return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B sessionEventSubscriber(Subscriber<? super SessionEvent> sessionEventSubscriber) {
      this.encoding = Objects.requireNonNull(encoding);
      return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B timeoutSeconds(int timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
      return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B uri(URI uri) {
      this.uri = Objects.requireNonNull(uri);
      return (B) this;
    }

  }

  public static Injector.Builder builder() {
    return new Injector.Builder();
  }

  private final ClientEndpoint endpoint;
  private final Consumer<Throwable> errorListener;

  private final Subscriber<? super SessionEvent> internalEventSubscriber = new Subscriber<>() {


    @Override
    public void onComplete() {

    }

    @Override
    public void onError(Throwable throwable) {
      errorListener.accept(throwable);
    }

    @Override
    public void onNext(SessionEvent item) {
      try {
        sessionStateLock.lock();
        sessionStateCondition.signalAll();
        switch (item.getState()) {
          case ESTABLISHED:
            System.out.println("Session established");
            break;
          case NEGOTIATED:
            System.out.println("Session negotiated");
            break;
          case FINALIZED:
            System.out.println("Session finalized");
            break;
          case NOT_ESTABLISHED:
            System.out.println("Session transport unbound");
            break;
          case NOT_NEGOTIATED:
            System.out.println("Session initialized");
            break;
          case FINALIZE_REQUESTED:
            System.out.println("Session finalizing");
            break;
        }
        request(1);
      } finally {
        sessionStateLock.unlock();
      }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      Trader.this.subscription = subscription;
      request(1);
    }

    private void request(int n) {
      subscription.request(n);
    }
  };

  private ApplicationMessageConsumer applicationMessageConsumer = null;
  private final BufferSupplier requestBufferSupplier = new BufferPool();
  private final MutableRequestMessageFactory requestFactory;
  private final ResponseMessageFactory responseFactory;
  private final RingBufferSupplier ringBuffer;
  private ClientSession session;
  private final Subscriber<? super SessionEvent> sessionEventSubscriber;
  private final SessionMessenger sessionMessenger;
  private final Condition sessionStateCondition;
  private final ReentrantLock sessionStateLock = new ReentrantLock();
  private Subscription subscription;
  private final int timeoutSeconds;
  private final Timer timer = new Timer("Client-timer", true);

  // Consumes messages from ring buffer
  private final BiConsumer<String, ByteBuffer> inboundMessageConsumer = (source, buffer) -> {
    try {
      session.messageReceived(buffer);
    } catch (Exception e) {
      if (getErrorListener() != null) {
        getErrorListener().accept(e);
      }
      session.disconnected();
    }
  };
  
  // Consumes application messages from Session
  private final SessionMessageConsumer sessionMessageConsumer = (source, buffer, seqNo) -> {
    Message message;
    try {
      message = getResponseFactory().wrap(buffer);
      applicationMessageConsumer.accept(source, message, seqNo);
    } catch (MessageException e) {
      getErrorListener().accept(e);
    }

  };
  private final long heartbeatInterval;
  
  protected Trader(@SuppressWarnings("rawtypes") Builder<? extends Trader, ? extends Trader.Builder>  builder) {
    this.applicationMessageConsumer =
        Objects.requireNonNull(builder.messageListener, "Message listener not set");
    this.errorListener = builder.errorListener;
    this.timeoutSeconds = builder.timeoutSeconds;
    this.ringBuffer = new RingBufferSupplier(inboundMessageConsumer);
    MessageProvider messageProvider = provider(builder.encoding);
    boolean isBinary = messageProvider.isBinary();
    this.requestFactory = messageProvider.getMutableRequestMessageFactory(requestBufferSupplier);
    this.responseFactory = messageProvider.getResponseMessageFactory();
    this.sessionMessenger = messageProvider.getSessionMessenger();
    sessionStateCondition = sessionStateLock.newCondition();
    this.sessionEventSubscriber = builder.sessionEventSubscriber;
    //Path outputPath = FileSystems.getDefault().getPath(builder.outputPath);
    this.heartbeatInterval = builder.heartbeatInterval;
    this.endpoint = new ClientEndpoint(ringBuffer, builder.uri, isBinary ? "binary" : "text",
        builder.timeoutSeconds);

  }


  public void close() {
    try {
      endpoint.close();
      ringBuffer.stop();
    } catch (Exception e) {
      errorListener.accept(e);
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


  /**
   * Opens a session with a server
   * <p>
   * A subscription for session events
   * 
   * @throws Exception if a session cannot be established
   */
  public void open() throws Exception {
    ringBuffer.start();
    if (session == null) {
      UUID uuid = UUID.randomUUID();
      this.session = ClientSession.builder().sessionId(Session.UUIDAsBytes(uuid)).timer(timer)
          .heartbeatInterval(heartbeatInterval)
          .sessionMessageConsumer(sessionMessageConsumer).sessionMessenger(sessionMessenger)
          .build();
      session.subscribeForEvents(internalEventSubscriber);
      if (sessionEventSubscriber != null) {
        session.subscribeForEvents(sessionEventSubscriber);
      }
    }

    endpoint.open();
    session.connected(endpoint, endpoint.getSource());
  }

  /**
   * Send an application message
   * <p>
   * Side effect: releases the underlying buffer of the MutableMessage if successful. If an
   * exception is thrown, the buffer is not release, allowing a retry.
   * 
   * @param message to send
   * @return sequence number of the sent message
   * @throws TimeoutException if the operation fails to complete in a timeout period
   * @throws InterruptedException if the current thread is interrupted
   * @throws IOException if an I/O error occurs
   */
  public long send(MutableMessage message) throws IOException, InterruptedException {
    Objects.requireNonNull(message);
    long seqNo = sendApplicationMessage(message.toBuffer());
    message.release();
    return seqNo;
  }

  /**
   * Sends a buffer holding an application message
   * 
   * @param buffer message buffer
   * @return sequence number of the sent message
   * @throws TimeoutException if the operation fails to complete in a timeout period
   * @throws InterruptedException if the current thread is interrupted
   * @throws IOException if an I/O error occurs
   * @throws IllegalStateException if this Session is not established
   */
  public long sendApplicationMessage(ByteBuffer buffer) throws IOException, InterruptedException {
    Objects.requireNonNull(buffer);
    return session.sendApplicationMessage(buffer);
  }

  public void suspend() {
    try {
      if (session != null) {
        try {
          sessionStateLock.lockInterruptibly();
          endpoint.close();
          while (session.getSessionState() != SessionState.NOT_ESTABLISHED) {
            sessionStateCondition.await(timeoutSeconds, TimeUnit.SECONDS);
          }
        } finally {
          sessionStateLock.unlock();
        }
      }
    } catch (Exception e) {
      errorListener.accept(e);
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Trader [session=").append(session).append(", endpoint=").append(endpoint)
        .append(", timeoutSeconds=").append(timeoutSeconds).append("]");
    return builder.toString();
  }

  private ResponseMessageFactory getResponseFactory() {
    return responseFactory;
  }

  /**
   * Locate a service provider for an application message encoding
   * 
   * @param name encoding name
   * @return a service provider
   */
  private MessageProvider provider(String name) {
    ServiceLoader<MessageProvider> loader = ServiceLoader.load(MessageProvider.class);
    for (MessageProvider provider : loader) {
      if (provider.name().equals(name)) {
        return provider;
      }
    }
    throw new RuntimeException("No MessageProvider found");
  }


  protected BufferSupplier getRequestBufferSupplier() {
    return requestBufferSupplier;
  }

  void cancelEventSubscription() {
    if (subscription != null) {
      subscription.cancel();
    }
  }

  Consumer<Throwable> getErrorListener() {
    return errorListener;
  }
}
