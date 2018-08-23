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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * A circular buffer of reusable buffers
 * 
 * <p>
 * Supports a single writer thread only.
 * 
 * @author Don Mendelson
 *
 */
public class RingBufferSupplier implements BufferSupplier {

  private class BufferEvent {
    private final ByteBuffer buffer;
    private String source = null;

    BufferEvent() {
      buffer = ByteBuffer.allocateDirect(capacity).order(order);
    }

    ByteBuffer getBuffer() {
      return buffer;
    }

    String getSource() {
      return source;
    }

    void setSource(String source) {
      this.source = source;
    }

  }

  public static final int DEFAULT_BUFFER_CAPACITY = 1024;
  public static final int DEFAULT_QUEUE_DEPTH = 64;

  private final int capacity;
  private final BiConsumer<String, ByteBuffer> consumer;
  private Disruptor<BufferEvent> disruptor;

  private final EventHandler<BufferEvent> eventHandler = new EventHandler<>() {

    @Override
    public void onEvent(BufferEvent event, long sequence, boolean endOfBatch) throws Exception {
      consumer.accept(event.getSource(), event.getBuffer());
    }

  };

  private final int queueDepth;

  private RingBuffer<BufferEvent> ringBuffer = null;

  private final BufferSupplier.BufferSupply supply = new BufferSupplier.BufferSupply() {

    private BufferEvent bufferEvent = null;
    private final AtomicReference<BufferEvent> eventRef = new AtomicReference<>();
    private long sequence;

    @Override
    public ByteBuffer acquire() {
      sequence = ringBuffer.next();
      if (eventRef.compareAndSet(null, ringBuffer.get(sequence))) {
        bufferEvent = eventRef.get();
        final ByteBuffer buffer = bufferEvent.getBuffer();
        buffer.clear();
        return buffer;
      } else {
        return null;
      }
    }

    @Override
    public String getSource() {
      if ((null != bufferEvent) && eventRef.compareAndSet(bufferEvent, bufferEvent)) {
        return bufferEvent.getSource();
      } else {
        throw new IllegalStateException("Buffer not acquired");
      }
    }

    @Override
    public void release() {
      if ((null != bufferEvent) && eventRef.compareAndSet(bufferEvent, null)) {
        ringBuffer.publish(sequence);
      }
    }

    @Override
    public void setSource(String source) {
      if ((null != bufferEvent) && eventRef.compareAndSet(bufferEvent, bufferEvent)) {
        bufferEvent.setSource(source);
      } else {
        throw new IllegalStateException("Buffer not acquired");
      }
    }

  };

  private final ThreadFactory threadFactory;
  private ByteOrder order;

  /**
   * Constructor with default thread factory and capacity
   * 
   * @param consumer handles queued buffers
   */
  public RingBufferSupplier(BiConsumer<String, ByteBuffer> consumer) {
    this(consumer, DEFAULT_BUFFER_CAPACITY, ByteOrder.nativeOrder(), DEFAULT_QUEUE_DEPTH, Executors.defaultThreadFactory());
  }

  /**
   * Constructor with default thread factory
   * 
   * @param consumer handles queued buffers
   * @param capacity capacity of each buffer. Should be a multiple of cache line.
   * @param queueDepth number of slots in the circular buffer. Must be a power of 2.
   */
  public RingBufferSupplier(BiConsumer<String, ByteBuffer> consumer, int capacity, int queueDepth) {
    this(consumer, capacity, ByteOrder.nativeOrder(), queueDepth, Executors.defaultThreadFactory());
  }

  /**
   * Constructor
   * 
   * @param consumer handles queued buffers
   * @param capacity capacity of each buffer. Should be a multiple of cache line.
   * @param queueDepth number of slots in the circular buffer. Must be a power of 2.
   * @param threadFactory creates a thread to dequeue buffers and invoke consumer
   */
  public RingBufferSupplier(BiConsumer<String, ByteBuffer> consumer, int capacity, ByteOrder order, int queueDepth,
      ThreadFactory threadFactory) {
    this.capacity = capacity;
    this.order = order;
    this.threadFactory = threadFactory;
    this.consumer = consumer;
    this.queueDepth = queueDepth;
  }

  @Override
  public BufferSupply get() {
    return supply;
  }

  public void start() {
    if (disruptor == null) {
      disruptor = new Disruptor<BufferEvent>(BufferEvent::new, queueDepth, threadFactory,
          ProducerType.SINGLE, new BusySpinWaitStrategy());

      // Connect the handler
      disruptor.handleEventsWith(eventHandler);

      // Start the Disruptor, starts all threads running
      disruptor.start();

      ringBuffer = disruptor.getRingBuffer();
    }
  }

  public void stop() {
    if (disruptor != null ) {
      disruptor.shutdown();
      disruptor = null;
    }
  }


}
