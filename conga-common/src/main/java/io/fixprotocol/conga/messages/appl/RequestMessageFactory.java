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

package io.fixprotocol.conga.messages.appl;

import java.nio.ByteBuffer;

/**
 * @author Don Mendelson
 *
 */
public interface RequestMessageFactory {
  
  /**
   * Attach a decoder of appropriate type to a message buffer
   * @param buffer a buffer with an encoded request message
   * @return a message decoder of appropriate type
   * @throws MessageException if unable to parse the message or determine its type
   */
  Message wrap(ByteBuffer buffer) throws MessageException;
  
  /**
   * Access a NewOrderSingle decoder for a wrapped message
   * @return a NewOrderSingle decoder
   */
  NewOrderSingle getNewOrderSingle();
  
  /**
   * Access a OrderCancelRequest decoder for a wrapped message
   * @return a OrderCancelRequest decoder
   */
  OrderCancelRequest getOrderCancelRequest();
  
  /**
   * Access a NotApplied decoder for a wrapped message
   * @return a NotApplied decoder
   */
  NotApplied getNotApplied();
}
