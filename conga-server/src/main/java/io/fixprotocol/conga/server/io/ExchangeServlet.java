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

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import io.fixprotocol.conga.buffer.RingBufferSupplier;
import io.fixprotocol.conga.server.session.ServerSessions;

/**
 * @author Don Mendelson
 *
 */
public class ExchangeServlet extends WebSocketServlet {

  private static final long serialVersionUID = -357978763515258850L;
  private final RingBufferSupplier ringBuffer;
  private final ServerSessions sessions;
  
  public ExchangeServlet(ServerSessions sessions, RingBufferSupplier ringBuffer) {
    this.sessions = sessions;
    this.ringBuffer = ringBuffer;
  }

  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.setCreator(new ExchangeSocketCreator(sessions, ringBuffer));
  }

}
