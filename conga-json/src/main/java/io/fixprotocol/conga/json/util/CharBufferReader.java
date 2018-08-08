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

package io.fixprotocol.conga.json.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.BufferUnderflowException;
import java.nio.CharBuffer;

/**
 * @author Don Mendelson
 *
 */
public class CharBufferReader extends Reader {

  private CharBuffer buffer;

  /**
   * 
   */
  public CharBufferReader(CharBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public void close() throws IOException {
    this.buffer = null;
  }

  @Override
  public void mark(int readAheadLimit) throws IOException {
    ensureOpen();
    buffer.mark();
  }

  @Override
  public boolean markSupported() {
    return true;
  }

  @Override
  public int read() throws IOException {
    ensureOpen();
    return buffer.get();
  }

  @Override
  public int read(char[] dst, int offset, int length) throws IOException {
    try {
      ensureOpen();
      int charsRead = Math.min(length, buffer.remaining());
      if (charsRead == 0) {
        // -1 indicates end of stream
        return -1;
      }
      buffer.get(dst, offset, charsRead);
      return charsRead;
    } catch (BufferUnderflowException | IndexOutOfBoundsException e) {
      throw new IOException(e);
    }

  }

  @Override
  public void reset() throws IOException {
    ensureOpen();
    buffer.reset();
  }

  @Override
  public long skip(long n) throws IOException {
    long toSkip = Math.min(n, buffer.remaining());
    buffer.position((int) (buffer.position() + toSkip));
    return toSkip;
  }

  private void ensureOpen() throws IOException {
    if (buffer == null) {
      throw new IOException("Stream closed");
    }
  }

}
