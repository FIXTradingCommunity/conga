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

package io.fixprotocol.conga.messages;

import io.fixprotocol.conga.messages.Message;

/**
 * Consumes application messages from a Session
 * 
 * @author Don Mendelson
 *
 */
public interface ApplicationMessageConsumer {
  
  /**
   * Consume an application message
   * @param source a session identifier
   * @param message a message to consume
   * @param seqNo sequence number of the message
   */
  void accept(String source, Message message, long seqNo);

}
