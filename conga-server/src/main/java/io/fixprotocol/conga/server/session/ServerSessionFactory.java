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

package io.fixprotocol.conga.server.session;

import java.util.Timer;

import io.fixprotocol.conga.session.SessionFactory;

/**
 * @author Don Mendelson
 *
 */
public class ServerSessionFactory implements SessionFactory {

  private final long heartbeatInterval;
  private final Timer timer;

  public ServerSessionFactory(Timer timer, long heartbeatInterval) {
    this.timer = timer;
    this.heartbeatInterval = heartbeatInterval;
  }

  @Override
  public ServerSession newInstance() {
    return new ServerSession(timer, heartbeatInterval);
  }

}
