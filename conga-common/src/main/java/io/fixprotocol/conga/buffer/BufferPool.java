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
import java.util.concurrent.ArrayBlockingQueue;

/**
 * A thread-safe, non-blocking, fixed-size buffer pool
 * 
 * @author Don Mendelson
 *
 */
public class BufferPool implements BufferSupplier {

  private class BufferPoolSupply implements BufferSupply {

    private final ByteBuffer buffer;
    private String source = null;

    BufferPoolSupply() {
      this.buffer = ByteBuffer.allocateDirect(capacity).order(order);
    }

    @Override
    public ByteBuffer acquire() {
      return buffer;
    }

    @Override
    public String getSource() {
      return source;
    }

    @Override
    public void release() {
      buffer.clear();
      pool.offer(this);
    }

    @Override
    public void setSource(String source) {
      this.source = source;
    }

  }

  private final int capacity;
  private final ArrayBlockingQueue<BufferPoolSupply> pool;
  private ByteOrder order;

  public static final int DEFAULT_BUFFER_CAPACITY = 1024;
  public static final int DEFAULT_POOL_SIZE = 16;
  
  /**
   * Constructor with default capacity and pool size
   */
  public BufferPool() {
    this(DEFAULT_BUFFER_CAPACITY, DEFAULT_POOL_SIZE, ByteOrder.nativeOrder());
  }

  /**
   * Constructor
   * 
   * @param capacity buffer capacity
   * @param poolSize number of buffers in the pool
   */
  public BufferPool(int capacity, int poolSize, ByteOrder order) {
    this.capacity = capacity;
    this.order = order;
    pool = new ArrayBlockingQueue<>(poolSize);
    for (int i = 0; i < poolSize; i++) {
      pool.add(newInstance());
    }
  }

  @Override
  public BufferSupply get() {
    BufferPoolSupply supply = pool.poll();
    if (null == supply) {
      supply = newInstance();
    }
    return supply;
  }

  private BufferPoolSupply newInstance() {
    return new BufferPoolSupply();
  }

}
