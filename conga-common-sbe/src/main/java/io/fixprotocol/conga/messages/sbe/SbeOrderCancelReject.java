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

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import io.fixprotocol.conga.messages.CxlRejReason;
import io.fixprotocol.conga.messages.OrdStatus;
import io.fixprotocol.conga.messages.OrderCancelReject;
import io.fixprotocol.conga.sbe.messages.appl.CxlRejReasonEnum;
import io.fixprotocol.conga.sbe.messages.appl.OrdStatusEnum;
import io.fixprotocol.conga.sbe.messages.appl.OrderCancelRejectDecoder;

/**
 * @author Don Mendelson
 *
 */
public class SbeOrderCancelReject implements OrderCancelReject, SbeMessageWrapper {

  private final OrderCancelRejectDecoder decoder = new OrderCancelRejectDecoder();
  private final DirectBuffer directBuffer = new UnsafeBuffer();
  private String source = null;

  @Override
  public String getClOrdId() {
    return decoder.clOrdId();
  }

  @Override
  public CxlRejReason getCxlRejReason() {
    CxlRejReasonEnum cxlRejReason = decoder.cxlRejReason();
    return CxlRejReason.valueOf(cxlRejReason.name());
  }

  @Override
  public String getOrderId() {
    return decoder.orderId();
  }

  @Override
  public OrdStatus getOrdStatus() {
    OrdStatusEnum ordStatus = decoder.ordStatus();
    return OrdStatus.valueOf(ordStatus.name());
  }

  @Override
  public String getSource() {
    return source;
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
  
  @Override
  public ByteBuffer toBuffer() {
    return directBuffer.byteBuffer();
  }

}
