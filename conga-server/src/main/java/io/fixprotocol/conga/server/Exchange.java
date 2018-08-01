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

package io.fixprotocol.conga.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.fixprotocol.conga.buffer.BufferPool;
import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.RingBufferSupplier;
import io.fixprotocol.conga.messages.appl.Message;
import io.fixprotocol.conga.messages.appl.MessageException;
import io.fixprotocol.conga.messages.appl.MutableMessage;
import io.fixprotocol.conga.messages.appl.MutableResponseMessageFactory;
import io.fixprotocol.conga.messages.appl.NewOrderSingle;
import io.fixprotocol.conga.messages.appl.OrderCancelRequest;
import io.fixprotocol.conga.messages.appl.RequestMessageFactory;
import io.fixprotocol.conga.messages.spi.MessageProvider;
import io.fixprotocol.conga.server.io.ExchangeSocketServer;
import io.fixprotocol.conga.server.match.MatchEngine;
import io.fixprotocol.conga.server.session.ServerSession;
import io.fixprotocol.conga.server.session.ServerSessionFactory;
import io.fixprotocol.conga.server.session.ServerSessions;
import io.fixprotocol.conga.session.SessionMessageConsumer;

/**
 * @author Don Mendelson
 *
 */
public class Exchange implements AutoCloseable {

  public static class Builder {

    private String contextPath = DEFAULT_ROOT_CONTEXT_PATH;
    private String encoding;
    private long heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;

    protected Builder() {

    }

    public Exchange build() {
      return new Exchange(this);
    }

    public Builder contextPath(String contextPath) {
      this.contextPath = contextPath;
      return this;
    }

    public Builder encoding(String encoding) {
      this.encoding = encoding;
      return this;
    }

    public Builder heartbeatInterval(long heartbeatInterval) {
      this.heartbeatInterval = heartbeatInterval;
      return this;
    }

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

  }

  public static final long DEFAULT_HEARTBEAT_INTERVAL = 2000L;
  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 8025;
  public static final String DEFAULT_ROOT_CONTEXT_PATH = "/";

  public static Builder builder() {
    return new Builder();
  }

  /**
   * @param args
   * @throws Exception if WebSocket server fails to start
   */
  public static void main(String[] args) throws Exception {
    // Use communication defaults and SBE encoding
    try (Exchange exchange = Exchange.builder().encoding("SBE").build()) {
      exchange.open();
      final Object monitor = new Object();

      new Thread(() -> {

        boolean running = true;
        while (running) {
          synchronized (monitor) {
            try {
              monitor.wait();
            } catch (InterruptedException e) {
              running = false;
            }
          }
        }
        exchange.close();
      });
    }
  }

  private final String contextPath;
  private Consumer<Throwable> errorListener = (t) -> t.printStackTrace(System.err);
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final String host;

  // Consumes messages from ring buffer
  private final BiConsumer<String, ByteBuffer> incomingMessageConsumer = new BiConsumer<>() {

    @Override
    public void accept(String source, ByteBuffer buffer) {
      final ServerSession session = sessions.getSession(source);
      try {
        session.messageReceived(buffer);
      } catch (Throwable t) {
        errorListener.accept(t);
      }
    }
  };
  private final RingBufferSupplier incomingRingBuffer;
  private final MatchEngine matchEngine;
  private final BufferSupplier outgoingBufferSupplier = new BufferPool();
  private final int port;
  private final RequestMessageFactory requestMessageFactory;
  private ExchangeSocketServer server = null;

  // Consumes application messages from Session
  private final SessionMessageConsumer sessionMessageConsumer = (source, buffer, seqNo) -> {
    Message message;
    try {
      message = getRequestMessageFactory().wrap(buffer);
      match(source, message);
    } catch (MessageException e) {
      errorListener.accept(e);
    }
  };

  private final ServerSessions sessions;

  private final Timer timer = new Timer("Server-timer", true);

  /**
   * Construct new exchange server.
   *
   * @param hostName hostName of the server. If {@code null}, then {@link #DEFAULT_HOST} is used.
   * @param port port of the server. When provided value is {@code 0}, default port
   *        ({@value #DEFAULT_PORT}) will be used, when {@code -1}, ephemeral port number will be
   *        used.
   * @param heartbeatInterval heartbeat interval in millis
   */
  private Exchange(Builder builder) {
    this.host = builder.host;
    this.port = builder.port;
    this.contextPath = builder.contextPath;
    this.incomingRingBuffer = new RingBufferSupplier(incomingMessageConsumer);
    MessageProvider messageProvider = MessageProvider.provider(builder.encoding);
    this.requestMessageFactory = messageProvider.getRequestMessageFactory();
    MutableResponseMessageFactory responseMessageFactory =
        messageProvider.getMutableResponseMessageFactory(outgoingBufferSupplier);
    this.matchEngine = new MatchEngine(responseMessageFactory);
    this.sessions = new ServerSessions(new ServerSessionFactory(messageProvider,
        sessionMessageConsumer, timer, executor, builder.heartbeatInterval));
  }

  @Override
  public void close() {
    executor.shutdown();
    server.stop();
    incomingRingBuffer.stop();
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public void match(String source, Message message) throws MessageException {
    List<MutableMessage> responses = Collections.emptyList();
    if (message instanceof NewOrderSingle) {
      responses = matchEngine.onOrder(source, (NewOrderSingle) message);
    } else if (message instanceof OrderCancelRequest) {
      responses = matchEngine.onCancelRequest(source, (OrderCancelRequest) message);
    }

    for (MutableMessage response : responses) {
      final ByteBuffer outgoingBuffer = response.toBuffer();
      final ServerSession session = sessions.getSession(response.getSource());
      try {
        session.sendApplicationMessage(outgoingBuffer);
      } catch (IOException | InterruptedException | IllegalStateException e) {
        session.disconnected();
      }
      response.release();
    }
  }

  public void open() throws Exception {
    incomingRingBuffer.start();
    String keyStorePath = "selfsigned.pkcs";
    String keyStorePassword = "storepassword";
    server = ExchangeSocketServer.builder().ringBufferSupplier(incomingRingBuffer).host(host)
        .port(port).keyStorePath(keyStorePath).keyStorePassword(keyStorePassword).sessions(sessions)
        .build();
    server.run();
  }

  public void setErrorListener(Consumer<Throwable> errorListener) {
    this.errorListener = Objects.requireNonNull(errorListener);
  }

  private RequestMessageFactory getRequestMessageFactory() {
    return requestMessageFactory;
  }

}
