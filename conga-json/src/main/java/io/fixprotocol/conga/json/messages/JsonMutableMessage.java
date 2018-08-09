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

package io.fixprotocol.conga.json.messages;



import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import com.google.gson.Gson;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.BufferSupplier.BufferSupply;
import io.fixprotocol.conga.messages.appl.MutableMessage;

/**
 * @author Don Mendelson
 *
 */
public class JsonMutableMessage implements MutableMessage {

  private final static Gson gson = new Gson();
  private final transient ByteBuffer buffer;
  private final transient BufferSupply bufferSupply;
  private transient String source;

  protected JsonMutableMessage() {
    this.bufferSupply = null;
    this.buffer = null;
  }
  
  /**
   * Constructor acquires a buffer
   */
  protected JsonMutableMessage(BufferSupplier bufferSupplier) {
    this.bufferSupply = bufferSupplier.get();
    this.buffer = this.bufferSupply.acquire();
  }

  public String getSource() {
    return source;
  }

  @Override
  public void release() {
    bufferSupply.release();
  }

  @Override
  public void setSource(String source) {
    this.source = source;
  }

  @Override
  public ByteBuffer toBuffer() {
    final CharBuffer charBuffer = buffer.asCharBuffer();
    gson.toJson(this, charBuffer);
    // CharBuffer position is independent of original ByteBuffer
    int charPos = charBuffer.position();
    buffer.position(charPos << 1);
    buffer.flip();
    return buffer;
  }

}
