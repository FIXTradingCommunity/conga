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
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Don Mendelson
 *
 */
public class ThreadLocalBufferSupplier implements BufferSupplier {

  public static final int DEFAULT_CAPACITY = 1024;

  private final ThreadLocal<BufferSupply> bufferSupplierThreadLocal = new ThreadLocal<>() {
    @Override
    protected BufferSupply initialValue() {
      return new BufferSupply() {

        private final AtomicBoolean isAcquired = new AtomicBoolean();
        private final ByteBuffer buffer =
            ByteBuffer.allocateDirect(capacity).order(order);
        private String source = null;

        @Override
        public ByteBuffer acquire() {
          return isAcquired.compareAndSet(false, true) ? buffer : null;
        }

        @Override
        public void release() {
          if (isAcquired.compareAndSet(true, false)) {
            buffer.clear();
          }
        }

        @Override
        public String getSource() {
          if (isAcquired.compareAndSet(true, true)) {
            return source;
          } else {
            throw new IllegalStateException("Buffer not acquired");
          }
        }

        @Override
        public void setSource(String source) {
          if (isAcquired.compareAndSet(true, true)) {
            this.source = source;
          } else {
            throw new IllegalStateException("Buffer not acquired");
          }
        }

      };
    }
  };

  private final int capacity;

  private ByteOrder order;


  public ThreadLocalBufferSupplier() {
    this.capacity = DEFAULT_CAPACITY;
  }

  public ThreadLocalBufferSupplier(int capacity, ByteOrder order) {
    this.capacity = capacity;
    this.order = order;
  }

  @Override
  public BufferSupply get() {
    return bufferSupplierThreadLocal.get();
  }

}
