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

package io.fixprotocol.conga.server.io.callback;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

/**
 * @author Don Mendelson
 *
 */
public interface ExchangeSocket extends Closeable {

  /**
   * Send a message synchronously
   * @param buffer message buffer send
   */
  void send(ByteBuffer buffer) throws IOException;

  /**
   * Send a message asynchronously
   * @param buffer message buffer to send
   * @return a future that waits for completion
   */
  Future<Void> sendAsync(ByteBuffer buffer);

}
