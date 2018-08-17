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

package io.fixprotocol.conga.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Encodes Simple Open Framing Header
 * <p>
 * Delimits messages for protocols that do not perform framing.
 * 
 * @author Don Mendelson
 *
 */
public class SofhEncoder {

  private final ByteBuffer buffer = ByteBuffer.allocateDirect(16);

  /**
   * Constructor allocates a buffer for the header
   */
  public SofhEncoder() {
    buffer.order(ByteOrder.BIG_ENDIAN);
  }

  public ByteBuffer encode(long messageLength, short encodingCode) {
    int offset = 0;
    buffer.putInt(offset, (int) (messageLength & 0xffffffff));
    buffer.putShort(offset+4, encodingCode);
    buffer.limit(offset+6);
    return buffer.duplicate();
  }
  
  public int encodedLength() {
    return 6;
  }

  public ByteBuffer getBuffer() {
    return buffer;
  }

}
