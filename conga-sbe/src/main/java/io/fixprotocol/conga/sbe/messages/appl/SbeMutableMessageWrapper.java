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

package io.fixprotocol.conga.sbe.messages.appl;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.messages.appl.MutableMessage;

/**
 * @author Don Mendelson
 *
 */
interface SbeMutableMessageWrapper {

  /**
   * Acquire a buffer to populate
   * 
   * @param bufferSupplier 
   * @return this MutableMessage
   */
  MutableMessage wrap(BufferSupplier bufferSupplier);
  
  /**
   * Returns the acquired BufferSupply.
   * 
   * It should be released with done with the message.
   * @return the wrapped BufferSupply
   */
  BufferSupplier.BufferSupply getBufferSupply();
}
