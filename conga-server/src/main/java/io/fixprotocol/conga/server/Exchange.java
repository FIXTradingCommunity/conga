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
import io.fixprotocol.conga.match.MatchEngine;
import io.fixprotocol.conga.messages.Message;
import io.fixprotocol.conga.messages.MessageException;
import io.fixprotocol.conga.messages.MutableMessage;
import io.fixprotocol.conga.messages.NewOrderSingle;
import io.fixprotocol.conga.messages.OrderCancelRequest;
import io.fixprotocol.conga.messages.RequestMessageFactory;
import io.fixprotocol.conga.messages.sbe.SbeMutableResponseMessageFactory;
import io.fixprotocol.conga.messages.sbe.SbeRequestMessageFactory;
import io.fixprotocol.conga.server.io.ExchangeSocketServer;
import io.fixprotocol.conga.server.session.ServerSession;
import io.fixprotocol.conga.server.session.ServerSessionFactory;
import io.fixprotocol.conga.server.session.ServerSessions;
import io.fixprotocol.conga.session.ProtocolViolationException;
import io.fixprotocol.conga.session.SessionMessageConsumer;

/**
 * @author Don Mendelson
 *
 */
public class Exchange implements AutoCloseable {

  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 8025;
  public static final String DEFAULT_ROOT_CONTEXT_PATH = "/";

  private static final Object monitor = new Object();

  /**
   * @param args
   * @throws Exception if WebSocket server fails to start
   */
  public static void main(String[] args) throws Exception {
    try (Exchange exchange = new Exchange(null, 0, null, 2000L)) {
      exchange.open();

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

  private String contextPath = DEFAULT_ROOT_CONTEXT_PATH;
  
  private Consumer<Throwable> errorListener = (t) -> t.printStackTrace(System.err);
  
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private String host = DEFAULT_HOST;

  // Consumes messages from ring buffer
  private final BiConsumer<String, ByteBuffer> incomingMessageConsumer = new BiConsumer<>() {

    @Override
    public void accept(String source, ByteBuffer buffer) {
      final ServerSession session = sessions.getSession(source);
      try {
      session.messageReceived(buffer);
      } catch (ProtocolViolationException | MessageException e) {
        errorListener.accept(e);
      }
    }
  };
  private final RingBufferSupplier incomingRingBuffer;
  private final MatchEngine matchEngine;
  private final BufferSupplier outgoingBufferSupplier = new BufferPool();
  private int port = DEFAULT_PORT;
  private final RequestMessageFactory requestMessageFactory = new SbeRequestMessageFactory();
  private ExchangeSocketServer server = null;
  // Consumes application messages from Session
  private SessionMessageConsumer sessionMessageConsumer = (source, buffer, seqNo) -> {
    Message message;
    try {
      message = requestMessageFactory.wrap(buffer);
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
  public Exchange(String hostName, int port, String contextPath, long heartbeatInterval) {
    if (null != hostName) {
      this.host = hostName;
    }
    if (0 != port) {
      this.port = port;
    }
    if (null != contextPath) {
      this.contextPath = contextPath;
    }
    this.incomingRingBuffer = new RingBufferSupplier(incomingMessageConsumer);
    this.matchEngine =
        new MatchEngine(new SbeMutableResponseMessageFactory(outgoingBufferSupplier));
    this.sessions = new ServerSessions(new ServerSessionFactory(sessionMessageConsumer,
        timer, executor, heartbeatInterval));
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

}
