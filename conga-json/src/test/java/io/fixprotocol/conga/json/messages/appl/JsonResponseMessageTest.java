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

package io.fixprotocol.conga.json.messages.appl;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fixprotocol.conga.buffer.SingleBufferSupplier;
import io.fixprotocol.conga.messages.appl.CxlRejReason;
import io.fixprotocol.conga.messages.appl.ExecType;
import io.fixprotocol.conga.messages.appl.ExecutionReport.Fill;
import io.fixprotocol.conga.messages.appl.MessageException;
import io.fixprotocol.conga.messages.appl.MutableExecutionReport;
import io.fixprotocol.conga.messages.appl.MutableExecutionReport.MutableFill;
import io.fixprotocol.conga.messages.appl.MutableOrderCancelReject;
import io.fixprotocol.conga.messages.appl.OrdStatus;
import io.fixprotocol.conga.messages.appl.OrderCancelReject;
import io.fixprotocol.conga.messages.appl.Side;

/**
 * @author Don Mendelson
 *
 */
public class JsonResponseMessageTest {

  private SingleBufferSupplier bufferSupplier;
  private JsonMutableResponseMessageFactory mutableFactory;
  private JsonResponseMessageFactory factory;

  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  public void setUp() throws Exception {
    final ByteBuffer buffer = ByteBuffer.allocate(1024);
    buffer.order(ByteOrder.nativeOrder());
    bufferSupplier = new SingleBufferSupplier(buffer);
    mutableFactory = new JsonMutableResponseMessageFactory(bufferSupplier);
    factory = new JsonResponseMessageFactory();
  }

  @Test
  public void orderCancelReject() throws MessageException {
    MutableOrderCancelReject mutableReject = mutableFactory.getOrderCancelReject();
    String clOrdId = "C0001";
    mutableReject.setClOrdId(clOrdId);
    CxlRejReason cxlRejReason = CxlRejReason.TooLateToCancel;
    mutableReject.setCxlRejReason(cxlRejReason);
    String orderId = "O0001";
    mutableReject.setOrderId(orderId);
    OrdStatus ordStatus = OrdStatus.Filled;
    mutableReject.setOrdStatus(ordStatus);
    Instant time = Instant.now();
    mutableReject.setTransactTime(time);
    ByteBuffer buffer = mutableReject.toBuffer();
    
    factory.wrap(buffer);
    OrderCancelReject reject = factory.getOrderCancelReject();
    assertEquals(clOrdId, reject.getClOrdId());
    assertEquals(cxlRejReason, reject.getCxlRejReason());
    assertEquals(orderId, reject.getOrderId());
    assertEquals(ordStatus, reject.getOrdStatus());
    assertEquals(time, reject.getTransactTime());
  }

  @Test
  public void executionReport() throws MessageException {
    MutableExecutionReport mutableReport = mutableFactory.getExecutionReport();
    String clOrdId = "C0001";
    mutableReport.setClOrdId(clOrdId);
    int cumQty = 7;
    mutableReport.setCumQty(cumQty);
    String execId = "E0001";
    mutableReport.setExecId(execId);
    ExecType execType = ExecType.Trade;
    mutableReport.setExecType(execType);
    int count = 2;
    mutableReport.setFillCount(count);
    int leavesQty = 0;
    mutableReport.setLeavesQty(leavesQty);
    String orderId = "O0001";
    mutableReport.setOrderId(orderId);
    OrdStatus ordStatus = OrdStatus.Filled;
    mutableReport.setOrdStatus(ordStatus);
    Side side = Side.Buy;
    mutableReport.setSide(side);
    String symbol = "XYZ";
    mutableReport.setSymbol(symbol);
    Instant time = Instant.now();
    mutableReport.setTransactTime(time); 
    mutableReport.setFillCount(2);
    MutableFill fill1 = mutableReport.nextFill();
    BigDecimal fillPx = new BigDecimal("12.34");
    fill1.setFillPx(fillPx);
    fill1.setFillQty(3);
    MutableFill fill2 = mutableReport.nextFill();
    fill2.setFillPx(fillPx);
    fill2.setFillQty(4);
    ByteBuffer buffer = mutableReport.toBuffer();
    
    factory.wrap(buffer);
    JsonExecutionReport report = factory.getExecutionReport();
    assertEquals(clOrdId, report.getClOrdId());
    assertEquals(cumQty,report.getCumQty());
    assertEquals(execId, report.getExecId());
    assertEquals(execType, report.getExecType());
    assertEquals(leavesQty, report.getLeavesQty());
    assertEquals(orderId, report.getOrderId());
    assertEquals(ordStatus, report.getOrdStatus());
    assertEquals(side, report.getSide());
    assertEquals(symbol, report.getSymbol());
    assertEquals(time, report.getTransactTime());
    Iterator<? extends Fill> fills = report.getFills();
    int fillQty = 0;
    while (fills.hasNext()) {
      Fill fill = fills.next();
      BigDecimal price = fill.getFillPx();
      fillQty += fill.getFillQty();
    }
    assertEquals(7, fillQty);
  }

}
