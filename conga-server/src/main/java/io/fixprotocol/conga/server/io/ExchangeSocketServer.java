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

package io.fixprotocol.conga.server.io;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import io.fixprotocol.conga.buffer.RingBufferSupplier;
import io.fixprotocol.conga.server.session.ServerSessions;

/**
 * @author Don Mendelson
 *
 */
public class ExchangeSocketServer {

  /**
   * Builds an instance of {@code ExchangeSocketServer}
   * <p>
   * Example:
   * 
   * <pre>
   * ExchangeSocketServer server =
   *     ExchangeSocketServer.builder().keyStorePath("/store").keyStorePassword("XYZ").build();
   * </pre>
   *
   */
  public static final class Builder {
    public ServerSessions sessions;
    private String host = "localhost";
    private String keyManagerPassword = null;
    private String keyStorePassword;
    private String keyStorePath;
    private int port = 8443;
    private RingBufferSupplier ringBuffer;

    private Builder() {

    }

    public ExchangeSocketServer build() {
      return new ExchangeSocketServer(this);
    }

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder keyManagerPassword(String keyManagerPassword) {
      this.keyManagerPassword = keyManagerPassword;
      return this;
    }

    public Builder keyStorePassword(String keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
      return this;
    }

    public Builder keyStorePath(String keyStorePath) {
      this.keyStorePath = keyStorePath;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder ringBufferSupplier(RingBufferSupplier ringBuffer) {
      this.ringBuffer = ringBuffer;
      return this;
    }

    public Builder sessions(ServerSessions sessions) {
      this.sessions = sessions;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private final String host;
  private final String keyManagerPassword;
  private final String keyStorePassword;
  private final String keyStorePath;
  private final int port;
  private final RingBufferSupplier ringBuffer;
  private final Server server = new Server();
  private final ServerSessions sessions;

  private ExchangeSocketServer(Builder builder) {
    this.keyStorePath = builder.keyStorePath;
    this.keyStorePassword = builder.keyStorePassword;
    this.keyManagerPassword = builder.keyManagerPassword;
    this.ringBuffer = builder.ringBuffer;
    this.host = builder.host;
    this.port = builder.port;
    this.sessions = builder.sessions;
  }

  public void init() {

    // connector configuration
    SslContextFactory sslContextFactory = new SslContextFactory();
    if (null != keyStorePath) {
      sslContextFactory.setKeyStorePath(keyStorePath);
    }
    if (keyStorePassword != null) {
      sslContextFactory.setKeyStorePassword(keyStorePassword);
    }
    if (keyManagerPassword != null) {
      sslContextFactory.setKeyManagerPassword(keyManagerPassword);
    }
    SslConnectionFactory sslConnectionFactory =
        new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());
    HttpConnectionFactory httpConnectionFactory =
        new HttpConnectionFactory(new HttpConfiguration());
    ServerConnector sslConnector =
        new ServerConnector(server, sslConnectionFactory, httpConnectionFactory);
    sslConnector.setHost(host);
    sslConnector.setPort(port);
    server.addConnector(sslConnector);

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    ServletHolder servletHolder = new ServletHolder(new ExchangeServlet(sessions, ringBuffer));
    context.addServlet(servletHolder, "/trade/*");
    // context.addServlet(DefaultServlet.class, "/");
    server.setHandler(context);
  }

  public void run() throws Exception {
    init();
    server.start();
    server.join();
  }

  public void stop() {
    try {
      server.stop();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}

