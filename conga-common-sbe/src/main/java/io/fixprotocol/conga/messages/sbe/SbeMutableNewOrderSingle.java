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

package io.fixprotocol.conga.messages.sbe;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.agrona.concurrent.UnsafeBuffer;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.BufferSupplier.BufferSupply;
import io.fixprotocol.conga.messages.MutableNewOrderSingle;
import io.fixprotocol.conga.messages.OrdType;
import io.fixprotocol.conga.messages.Side;
import io.fixprotocol.conga.sbe.messages.appl.MessageHeaderEncoder;
import io.fixprotocol.conga.sbe.messages.appl.NewOrderSingleEncoder;
import io.fixprotocol.conga.sbe.messages.appl.OrdTypeEnum;
import io.fixprotocol.conga.sbe.messages.appl.SideEnum;

/**
 * @author Don Mendelson
 *
 */
public class SbeMutableNewOrderSingle implements MutableNewOrderSingle, SbeMutableMessageWrapper {

  private BufferSupply bufferSupply = null;
  private final NewOrderSingleEncoder encoder = new NewOrderSingleEncoder();
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
  public void setOrderQty(int orderQty) {
    encoder.orderQty().mantissa(orderQty);
  }

  @Override
  public void setOrdType(OrdType ordType) {
    OrdTypeEnum value = OrdTypeEnum.valueOf(ordType.name());
    encoder.ordType(value);
  }

  @Override
  public void setPrice(BigDecimal price) {
    encoder.price().mantissa(price.unscaledValue().intValue());
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
  public SbeMutableNewOrderSingle wrap(BufferSupplier bufferSupplier) {
    this.bufferSupply = bufferSupplier.get();
    ByteBuffer buffer = bufferSupply.acquire();
    mutableBuffer.wrap(buffer);
    int offset = buffer.position();
    encoder.wrapAndApplyHeader(mutableBuffer, offset, headerEncoder);
    return this;
  }
}
