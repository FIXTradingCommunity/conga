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

package io.fixprotocol.conga.sbe.messages.appl;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.agrona.concurrent.UnsafeBuffer;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.BufferSupplier.BufferSupply;
import io.fixprotocol.conga.messages.appl.MutableOrderCancelRequest;
import io.fixprotocol.conga.messages.appl.Side;

/**
 * @author Don Mendelson
 *
 */
public class SbeMutableOrderCancelRequest
    implements MutableOrderCancelRequest, SbeMutableMessageWrapper {

  private BufferSupply bufferSupply = null;
  private final OrderCancelRequestEncoder encoder = new OrderCancelRequestEncoder();
  private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
  private final UnsafeBuffer mutableBuffer = new UnsafeBuffer();

  @Override
  public BufferSupply getBufferSupply() {
    return bufferSupply;
  }


  @Override
  public String getSource() {
    return bufferSupply.getSource();
  }

  @Override
  public void release() {
    bufferSupply.release();
  }

  @Override
  public void setClOrdId(String clOrdId) {
    encoder.clOrdId(clOrdId);
  }

  @Override
  public void setSide(Side side) {
    SideEnum value = SideEnum.valueOf(side.name());
    encoder.side(value);
  }

  public void setSource(String source) {
    bufferSupply.setSource(source);
  }

  @Override
  public void setSymbol(String symbol) {
    encoder.symbol(symbol);
  }

  @Override
  public void setTransactTime(Instant transactTime) {
    long value = TimeUnit.SECONDS.toNanos(transactTime.getEpochSecond()) + transactTime.getNano();
    encoder.transactTime().time(value);
  }

  @Override
  public ByteBuffer toBuffer() {
    ByteBuffer buffer = mutableBuffer.byteBuffer();
    buffer.limit(headerEncoder.encodedLength() + encoder.encodedLength());
    return buffer;
  }

  @Override
  public SbeMutableOrderCancelRequest wrap(BufferSupplier bufferSupplier) {
    this.bufferSupply = bufferSupplier.get();
    ByteBuffer buffer = bufferSupply.acquire();
    mutableBuffer.wrap(buffer);
    int offset = buffer.position();
    encoder.wrapAndApplyHeader(mutableBuffer, offset, headerEncoder);
    return this;
  }
}
