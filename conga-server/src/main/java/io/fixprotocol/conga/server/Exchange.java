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
import java.util.function.BiConsumer;

import io.fixprotocol.conga.buffer.BufferPool;
import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.RingBufferSupplier;
import io.fixprotocol.conga.match.MatchEngine;
import io.fixprotocol.conga.messages.*;
import io.fixprotocol.conga.messages.sbe.SbeMutableResponseMessageFactory;
import io.fixprotocol.conga.messages.sbe.SbeRequestMessageFactory;
import io.fixprotocol.conga.server.io.BinaryExchangeSocket;
import io.fixprotocol.conga.server.io.ExchangeSocketServer;

/**
 * @author Don Mendelson
 *
 */
public class Exchange implements AutoCloseable {

  public static final String DEFAULT_ROOT_CONTEXT_PATH = "/";
  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 8025;

  private static final Object monitor = new Object();
  
  private final BiConsumer<String, ByteBuffer> incomingMessageConsumer = new BiConsumer<>() {

    @Override
    public void accept(String source, ByteBuffer buffer) {
      List<MutableMessage> responses = Collections.emptyList();
      try {
        Message message = messageFactory.wrap(buffer);
        if (message instanceof NewOrderSingle) {
          responses = matchEngine.onOrder(source, (NewOrderSingle) message);
        } else if (message instanceof OrderCancelRequest) {
          responses = matchEngine.onCancelRequest(source, (OrderCancelRequest) message);
        } else {
          // Unknown message type
        }
      } catch (MessageException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      for (MutableMessage response : responses) {
        try {
          final ByteBuffer outgoingBuffer = response.toBuffer();
          BinaryExchangeSocket.send(response.getSource(), outgoingBuffer);
          response.release();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    
  };
  private final RingBufferSupplier incomingRingBuffer;
  private final MatchEngine matchEngine;
  private final RequestMessageFactory messageFactory = new SbeRequestMessageFactory();
  
  /**
   * @param args
   * @throws Exception if WebSocket server fails to start
   */
  public static void main(String[] args) throws Exception {
    try (Exchange exchange = new Exchange(null, 0, null)) {
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

  private String host = DEFAULT_HOST;
  private int port = DEFAULT_PORT;
  private String contextPath = DEFAULT_ROOT_CONTEXT_PATH;
  private ExchangeSocketServer server = null;
  private final BufferSupplier outgoingBufferSupplier = new BufferPool();

  /**
   * Construct new exchange server.
   *
   * @param hostName hostName of the server. If {@code null}, then {@link #DEFAULT_HOST} is
   *        used.
   * @param port port of the server. When provided value is {@code 0}, default port
   *        ({@value #DEFAULT_PORT}) will be used, when {@code -1}, ephemeral port number will be
   *        used.
   */
  public Exchange(String hostName, int port, String contextPath) {
    if (null != hostName) {
      this.host = hostName;
    }
    if (port != 0) {
      this.port = port;
    }
    if (contextPath != null) {
      this.contextPath = contextPath;
    }
    incomingRingBuffer = new RingBufferSupplier(incomingMessageConsumer);
    matchEngine = new MatchEngine(new SbeMutableResponseMessageFactory(outgoingBufferSupplier));
  }

  @Override
  public void close() {
    server.stop();
    incomingRingBuffer.stop();
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public void open() throws Exception {
    incomingRingBuffer.start();
    String keyStorePath = "selfsigned.pkcs";
    String keyStorePassword = "storepassword";
    server =
        ExchangeSocketServer.builder().ringBufferSupplier(incomingRingBuffer)
        .host(host).port(port).keyStorePath(keyStorePath).keyStorePassword(keyStorePassword).build();
    server.run();
  }

}
