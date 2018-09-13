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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.function.Supplier;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.BufferSupplier.BufferSupply;

/**
 * Supplies populated message buffers from a log
 * 
 * @author Don Mendelson
 *
 */
public class LogMessageSupplier implements Supplier<BufferSupply>, Closeable  {

  private final BufferSupplier bufferSupplier;
  private short lastEncoding;
  private final MessageLogReader reader;

  /**
   * Constructor
   * 
   * @param path file path of log
   * @param bufferSupplier supplies buffers to populate
   * @throws IOException if the log cannot be read
   */
  public LogMessageSupplier(Path path, BufferSupplier bufferSupplier) throws IOException {
    reader = new MessageLogReader(path);
    this.bufferSupplier = bufferSupplier;
  }

  public void close() throws IOException {
    reader.close();
  }

  @Override
  public BufferSupply get() {
    final BufferSupply supply = bufferSupplier.get();
    final ByteBuffer buffer = supply.acquire();
    try {
      int bytesRead = reader.read(buffer);
      if (bytesRead > 0) {
        this.lastEncoding = reader.getLastEncoding();
        buffer.flip();
        return supply;
      } else {
        supply.release();
        return null;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the encoding code of the last message read
   * 
   * @return code defined by SOFH
   */
  public short getLastEncoding() {
    return lastEncoding;
  }

  public void open() throws IOException {
    reader.open();
  }

}
