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

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import io.fixprotocol.conga.messages.appl.NewOrderSingle;
import io.fixprotocol.conga.messages.appl.OrdType;
import io.fixprotocol.conga.messages.appl.Side;

/**
 * @author Don Mendelson
 *
 */
public class SbeNewOrderSingle implements NewOrderSingle, SbeMessageWrapper {

  private final NewOrderSingleDecoder decoder = new NewOrderSingleDecoder();
  private final DirectBuffer directBuffer = new UnsafeBuffer();
  private String source = null;

  @Override
  public String getClOrdId() {
    return decoder.clOrdId();
  }

  @Override
  public int getOrderQty() {
    return decoder.orderQty().mantissa();
  }

  @Override
  public OrdType getOrdType() {
    OrdTypeEnum ordType = decoder.ordType();
    return OrdType.valueOf(ordType.name());
  }

  @Override
  public BigDecimal getPrice() {
    return BigDecimal.valueOf(decoder.price().mantissa(), -decoder.price().exponent());
  }

  @Override
  public Side getSide() {
    SideEnum side = decoder.side();
    return Side.valueOf(side.name());
  }

  @Override
  public String getSource() {
    return source;
  }

  @Override
  public String getSymbol() {
    return decoder.symbol();
  }

  @Override
  public Instant getTransactTime() {
    long seconds = TimeUnit.NANOSECONDS.toSeconds(decoder.transactTime().time());
    long nanos = decoder.transactTime().time() % TimeUnit.SECONDS.toNanos(1);
    return Instant.ofEpochSecond(seconds, nanos);
  }

  public void setSource(String source) {
    this.source = source;
  }

  @Override
  public void wrap(ByteBuffer buffer, int offset, int actingBlockLength, int actingVersion) {
    directBuffer.wrap(buffer);
    decoder.wrap(directBuffer, offset, actingBlockLength, actingVersion);
  }

}
