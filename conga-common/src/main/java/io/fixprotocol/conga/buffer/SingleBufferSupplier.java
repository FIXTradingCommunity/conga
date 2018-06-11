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

package io.fixprotocol.conga.buffer;

import java.nio.ByteBuffer;

/**
 * A buffer supplier suitable for testing or single-threaded operations
 * 
 * <p>
 * Not thread-safe
 * 
 * @author Don Mendelson
 *
 */
public class SingleBufferSupplier implements BufferSupplier {

  private final ByteBuffer buffer;
  
  private final BufferSupplier.BufferSupply supply = new BufferSupplier.BufferSupply() {

    private String source = null;

    @Override
    public ByteBuffer acquire() {
      return buffer;
    }

    @Override
    public void release() {
      buffer.clear();
    }

    @Override
    public String getSource() {
      return source;
    }

    @Override
    public void setSource(String source) {
      this.source = source;   
    }

  };

  /**
   * Constructor
   * 
   * @param buffer a buffer to supply
   */
  public SingleBufferSupplier(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public BufferSupply get() {
    return supply;
  }

}
