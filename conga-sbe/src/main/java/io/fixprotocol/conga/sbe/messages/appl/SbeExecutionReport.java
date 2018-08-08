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
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import io.fixprotocol.conga.messages.appl.ExecType;
import io.fixprotocol.conga.messages.appl.ExecutionReport;
import io.fixprotocol.conga.messages.appl.OrdStatus;
import io.fixprotocol.conga.messages.appl.Side;
import io.fixprotocol.conga.sbe.messages.appl.ExecutionReportDecoder.FillsGrpDecoder;

/**
 * @author Don Mendelson
 *
 */
public class SbeExecutionReport implements ExecutionReport, SbeMessageWrapper {


  private static class SbeFill implements ExecutionReport.Fill {

    private final FillsGrpDecoder fillsDecoder;

    SbeFill(FillsGrpDecoder fillsDecoder) {
      this.fillsDecoder = fillsDecoder;
    }

    @Override
    public BigDecimal getFillPx() {
      return BigDecimal.valueOf(fillsDecoder.fillPx().mantissa(),
          -fillsDecoder.fillPx().exponent());

    }

    @Override
    public int getFillQty() {
      return fillsDecoder.fillQty().mantissa();
    }

  }

  private static class SbeFillIterator implements Iterator<Fill> {

    private FillsGrpDecoder fillsDecoder;

    SbeFillIterator(FillsGrpDecoder fillsDecoder) {
      this.fillsDecoder = fillsDecoder;
    }

    @Override
    public boolean hasNext() {
      return fillsDecoder.hasNext();
    }

    @Override
    public Fill next() {
      fillsDecoder = fillsDecoder.next();
      return new SbeFill(fillsDecoder);
    }

  }

  private final ExecutionReportDecoder decoder = new ExecutionReportDecoder();
  private final DirectBuffer directBuffer = new UnsafeBuffer();
  private String source = null;

  @Override
  public String getClOrdId() {
    return decoder.clOrdId();
  }

  @Override
  public int getCumQty() {
    return decoder.cumQty().mantissa();
  }

  @Override
  public String getExecId() {
    return decoder.execId();
  }

  @Override
  public ExecType getExecType() {
    ExecTypeEnum execType = decoder.execType();
    return ExecType.valueOf(execType.name());
  }

  @Override
  public Iterator<Fill> getFills() {
    return new SbeFillIterator(decoder.fillsGrp());
  }

  @Override
  public int getLeavesQty() {
    return decoder.leavesQty().mantissa();
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
