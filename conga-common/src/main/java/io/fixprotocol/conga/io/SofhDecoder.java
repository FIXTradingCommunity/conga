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

/**
 * Decodes Simple Open Framing Header
 * <p>
 * Delimits messages for protocols that do not perform framing.
 * 
 * @author Don Mendelson
 *
 */
public class SofhDecoder {

  private ByteBuffer buffer = ByteBuffer.allocateDirect(16);

  public SofhDecoder() {
    buffer.limit(6);
  }

  public int encodedLength() {
    return 6;
  }

  public short encoding() {
    return buffer.getShort(4);
  }

  public ByteBuffer getBuffer() {
    return buffer.duplicate();
  }

  public long messageLength() {
    return buffer.getInt(0);
  }

}
