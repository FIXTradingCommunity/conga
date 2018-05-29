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
import io.fixprotocol.conga.messages.ExecType;
import io.fixprotocol.conga.messages.MutableExecutionReport;
import io.fixprotocol.conga.messages.OrdStatus;
import io.fixprotocol.conga.messages.Side;
import io.fixprotocol.conga.sbe.messages.appl.ExecTypeEnum;
import io.fixprotocol.conga.sbe.messages.appl.ExecutionReportEncoder;
import io.fixprotocol.conga.sbe.messages.appl.ExecutionReportEncoder.FillsGrpEncoder;
import io.fixprotocol.conga.sbe.messages.appl.MessageHeaderEncoder;
import io.fixprotocol.conga.sbe.messages.appl.OrdStatusEnum;
import io.fixprotocol.conga.sbe.messages.appl.SideEnum;

/**
 * @author Don Mendelson
 *
 */
public class SbeMutableExecutionReport implements MutableExecutionReport, SbeMutableMessageWrapper  {

  private class SbeMutableFill implements MutableExecutionReport.MutableFill {

    public void setFillPx(BigDecimal fillPx) {
      fillsGrpEncoder.fillPx().mantissa(fillPx.unscaledValue().intValue());
    }

    public void setFillQty(int fillQty) {
      fillsGrpEncoder.fillQty().mantissa(fillQty);
    }
  }

  private BufferSupply bufferSupply = null;
  private final ExecutionReportEncoder encoder = new ExecutionReportEncoder();
  private FillsGrpEncoder fillsGrpEncoder;
  private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
  private final UnsafeBuffer mutableBuffer = new UnsafeBuffer();

  @Override
  public String getSource() {
    return bufferSupply.getSource();
  }

  @Override
  public MutableFill nextFill() {
    fillsGrpEncoder = fillsGrpEncoder.next();
    return new SbeMutableFill();
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
  public void setCumQty(int cumQty) {
    encoder.cumQty().mantissa(cumQty);
  }

  @Override
  public void setExecId(String execId) {
    encoder.execId(execId);
  }

  @Override
  public void setExecType(ExecType execType) {
    ExecTypeEnum value = ExecTypeEnum.valueOf(execType.name());
    encoder.execType(value);
  }

  public void setFillCount(int count) {
    fillsGrpEncoder = encoder.fillsGrpCount(count);
  }

  @Override
  public void setLeavesQty(int leavesQty) {
    encoder.leavesQty().mantissa(leavesQty);
  }

  @Override
  public void setOrderId(String orderId) {
    encoder.orderId(orderId);
  }

  @Override
  public void setOrdStatus(OrdStatus ordStatus) {
    OrdStatusEnum value = OrdStatusEnum.valueOf(ordStatus.name());
    encoder.ordStatus(value);
  }

  @Override
  public void setSide(Side side) {
    SideEnum value = SideEnum.valueOf(side.name());
    encoder.side(value);
  }

  @Override
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
  public SbeMutableExecutionReport wrap(BufferSupplier bufferSupplier) {
    this.bufferSupply = bufferSupplier.get();
    ByteBuffer buffer = bufferSupply.acquire();
    mutableBuffer.wrap(buffer);
    int offset = buffer.position();
    encoder.wrapAndApplyHeader(mutableBuffer, offset, headerEncoder);
    return this;
  }

  @Override
  public ByteBuffer toBuffer() {
    ByteBuffer buffer = mutableBuffer.byteBuffer();
    buffer.limit(headerEncoder.encodedLength() + encoder.encodedLength());
    return buffer;
  }

  @Override
  public BufferSupply getBufferSupply() {
    return bufferSupply;
  }

}
