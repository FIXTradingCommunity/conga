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
import java.nio.ByteOrder;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.fixprotocol.conga.buffer.BufferPool;
import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.RingBufferSupplier;
import io.fixprotocol.conga.io.MessageLogWriter;
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
public class Exchange implements Runnable, AutoCloseable {

  public static class Builder {

    public static final String DEFAULT_ENCODING = "SBE";
    public static final long DEFAULT_HEARTBEAT_INTERVAL = 2000L;
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 8025;
    public static final String DEFAULT_ROOT_CONTEXT_PATH = "/";
    private static final String DEFAULT_OUTPUT_PATH = "log";
    
    public String outputPath = DEFAULT_OUTPUT_PATH;
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
      this.contextPath = Objects.requireNonNull(contextPath);
      return this;
    }

    public Builder encoding(String encoding) {
      this.encoding = Objects.requireNonNull(encoding);
      return this;
    }

    /**
     * Set heartbeat interval for new sessions
     * @param heartbeatInterval keepalive interval in millis
     * @return this Builder
     */
    public Builder heartbeatInterval(long heartbeatInterval) {
      this.heartbeatInterval = heartbeatInterval;
      return this;
    }

    public Builder host(String host) {
      this.host = Objects.requireNonNull(host);
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * @param args command line arguments. Execute with parameter {@code --help} to display all options.
   * @throws Exception if WebSocket server fails to start
   */
  public static void main(String[] args) throws Exception {

    final Builder builder = Exchange.builder();
    buildFromArgs(builder, args);
    
    try (Exchange exchange = builder.build()) {
      exchange.open();
      exchange.run();
    }
  }
  
  private static void buildFromArgs(Builder builder, String[] args) {
    Options options = new Options();
    options.addOption("i", "input", true, "path of input file");
    options.addOption("o", "output", true, "path of output file");
    options.addOption("e", "encoding", true, "message encoding");
    options.addOption("c", "contextpath", true, "context path");
    options.addOption("h", "host", true, "local host");
    options.addOption(Option.builder("p").longOpt("port").hasArg(true).desc("listen port")
        .type(Number.class).build());
    options.addOption(Option.builder("k").longOpt("keepalive").hasArg(true).desc("keepalive interval millis")
        .type(Number.class).build());
    options.addOption("?", "help", false, "disply usage");

    DefaultParser parser = new DefaultParser();
    CommandLine cmd;
    try {
      cmd = parser.parse(options, args);

      if (cmd.hasOption("?")) {
        usage(options);
        System.exit(0);
      }
      // String output = cmd.getOptionValue("o");
      // builder.outputPath(output);
      if (cmd.hasOption("e")) {
        String encoding = cmd.getOptionValue("e");
        builder.encoding(encoding);
      }
      if (cmd.hasOption("a")) {
        String apiPath = cmd.getOptionValue("a");
        builder.contextPath(apiPath);
      }
      if (cmd.hasOption("h")) {
        String host = cmd.getOptionValue("h");
        builder.host(host);
      }
      if (cmd.hasOption("p")) {
        Number port = (Number) cmd.getParsedOptionValue("p");
        builder.port(port.intValue());
      }
      if (cmd.hasOption("k")) {
        Number keepalive = (Number) cmd.getParsedOptionValue("l");
        builder.heartbeatInterval(keepalive.longValue());
      }
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      usage(options);
      System.exit(1);
    }
  }

  private static void usage(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Injector", options);
  }

  private final String contextPath;
  private final short encodingType;
  private Consumer<Throwable> errorListener = (t) -> t.printStackTrace(System.err);
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final String host;
  private final MessageLogWriter inboundLogWriter;
  private final RingBufferSupplier inboundRingBuffer;
  
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
  private final MatchEngine matchEngine;
  private final BufferSupplier outboundBufferSupplier = new BufferPool();
  private final MessageLogWriter outboundLogWriter;
  private final int port;
  private final RequestMessageFactory requestMessageFactory;
  private ExchangeSocketServer server = null;
  
  // Consumes incoming application messages from Session
  private final SessionMessageConsumer sessionMessageConsumer = (source, buffer, seqNo) -> {
    Message message;
    try {
      getInboundLogWriter().writeAsync(buffer.duplicate(), encodingType);
      message = getRequestMessageFactory().wrap(buffer);
      match(source, message);
    } catch (MessageException e) {
      errorListener.accept(e);
    }
  };
  private final ServerSessions sessions;
  private final Timer timer = new Timer("Server-timer", true);
  
  private Exchange(Builder builder) {
    this.host = builder.host;
    this.port = builder.port;
    this.contextPath = builder.contextPath;
    // Jetty uses big-endian buffers for receiving but converts them to byte[]
    this.inboundRingBuffer = new RingBufferSupplier(incomingMessageConsumer, 1024, ByteOrder.nativeOrder(), 64,
        Executors.defaultThreadFactory());
    MessageProvider messageProvider = provider(builder.encoding);
    encodingType = messageProvider.encodingType();
    this.requestMessageFactory = messageProvider.getRequestMessageFactory();
    MutableResponseMessageFactory responseMessageFactory =
        messageProvider.getMutableResponseMessageFactory(outboundBufferSupplier);
    this.matchEngine = new MatchEngine(responseMessageFactory);
    this.sessions = new ServerSessions(new ServerSessionFactory(messageProvider,
        sessionMessageConsumer, timer, executor, builder.heartbeatInterval));
    Path outputPath = FileSystems.getDefault().getPath(builder.outputPath);
    this.inboundLogWriter = new MessageLogWriter(outputPath.resolve("inbound.log"), false);
    this.outboundLogWriter = new MessageLogWriter(outputPath.resolve("outbound.log"), false);
  }
  
  @Override
  public void close() {
    executor.shutdown();
    if (server != null) {
      server.stop();
    }
    inboundRingBuffer.stop();
    try {
      getInboundLogWriter().close();
      getOutboundLogWriter().close();
    } catch (IOException e) {
      errorListener.accept(e);
    }
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
      final ByteBuffer outboundBuffer = response.toBuffer();
      final ServerSession session = sessions.getSession(response.getSource());
      try {
        session.sendApplicationMessage(outboundBuffer);
        outboundBuffer.flip();
        outboundLogWriter.writeAsync(outboundBuffer, encodingType).handle((l,e) -> {
          response.release();
          return l;
        });
      } catch (IOException | InterruptedException e) {
        session.disconnected();
        response.release();
      }
    }
  }

  public void open() throws Exception {
    inboundRingBuffer.start();
    getInboundLogWriter().open();
    getOutboundLogWriter().open();
    String keyStorePath = "selfsigned.pkcs";
    String keyStorePassword = "storepassword";
    server = ExchangeSocketServer.builder().ringBufferSupplier(inboundRingBuffer).host(host)
        .port(port).keyStorePath(keyStorePath).keyStorePassword(keyStorePassword).sessions(sessions)
        .build();
    server.run();
  }

  @Override
  public void run() {
    final Object monitor = new Object();
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
  }

  public void setErrorListener(Consumer<Throwable> errorListener) {
    this.errorListener = Objects.requireNonNull(errorListener);
  }

  private RequestMessageFactory getRequestMessageFactory() {
    return requestMessageFactory;
  }

  /**
   * Locate a service provider for an application message encoding
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

  MessageLogWriter getInboundLogWriter() {
    return inboundLogWriter;
  }

  MessageLogWriter getOutboundLogWriter() {
    return outboundLogWriter;
  }

}
