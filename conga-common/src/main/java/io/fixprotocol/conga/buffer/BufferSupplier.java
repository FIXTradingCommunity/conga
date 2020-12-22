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

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 * Buffer manager
 * 
 * <p>
 * Implementations are free to set policies for allocation, deallocation, pooling and thread safety.
 * 
 * @author Don Mendelson
 *
 */
@FunctionalInterface
public interface BufferSupplier extends Supplier<BufferSupplier.BufferSupply> {

  /**
   * Manages the lifetime of a single buffer provided by a BufferSupplier
   */
  interface BufferSupply {
    /**
     * Acquire a buffer and populate it from a source byte array
     * 
     * Transfers the bytes remaining in the source buffer into the acquired buffer.
     * 
     * @param src source byte array to copy
     * @param offset offset of the first byte in the source byte array to copy
     * @param length number of bytes to copy
     * @return the acquired buffer after the source bytes are copied into it or {@code null} if
     *         acquisition was unsuccessful
     * @throws BufferOverflowException If there is insufficient space in the acquired buffer for the
     *         source bytes
     */
    default ByteBuffer acquireAndCopy(byte[] src, int offset, int length) {
      ByteBuffer dest = acquire();
      if (null != dest) {
        dest.clear();
        dest.put(src, offset, length);
        dest.flip();
        return dest;
      } else {
        return null;
      }
    }

    /**
     * Acquire a buffer and populate it from a source buffer
     * 
     * Transfers the bytes remaining in the source buffer into the acquired buffer.
     * 
     * @param src source buffer to copy
     * @return the acquired buffer after the source bytes are copied into it or {@code null} if
     *         acquisition was unsuccessful
     * @throws BufferOverflowException If there is insufficient space in the acquired buffer for the
     *         remaining bytes in the source buffer
     */
    default ByteBuffer acquireAndCopy(ByteBuffer src) {
      ByteBuffer dest = acquire();
      if (null != dest) {
        dest.clear();
        dest.put(src);
        dest.flip();
        return dest;
      } else {
        return null;
      }
    }

    /**
     * Acquire a buffer to populate
     * 
     * @return a buffer if successful, or {@code null} if a buffer is unavailable
     */
    ByteBuffer acquire();

    /**
     * Get the source of data
     * 
     * @return an identifier of a data source
     * @throws IllegalStateException if invoked when a buffer has not been acquired.
     */
    String getSource();

    /**
     * Release the buffer for possible reuse
     * 
     * This is necessary because Java does not support automatic resource deallocation.
     */
    void release();

    /**
     * Set the source of data
     * 
     * Only valid after a buffer has been acquired.
     * 
     * @param source an identifier of a data source
     * @throws IllegalStateException if invoked when a buffer has not been acquired.
     */
    void setSource(String source);
  }

}
