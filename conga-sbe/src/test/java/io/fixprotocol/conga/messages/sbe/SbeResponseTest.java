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



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import io.fixprotocol.conga.buffer.SingleBufferSupplier;
import io.fixprotocol.conga.messages.appl.CxlRejReason;
import io.fixprotocol.conga.messages.appl.ExecType;
import io.fixprotocol.conga.messages.appl.ExecutionReport;
import io.fixprotocol.conga.messages.appl.Message;
import io.fixprotocol.conga.messages.appl.MessageException;
import io.fixprotocol.conga.messages.appl.MutableExecutionReport;
import io.fixprotocol.conga.messages.appl.MutableOrderCancelReject;
import io.fixprotocol.conga.messages.appl.OrdStatus;
import io.fixprotocol.conga.messages.appl.OrderCancelReject;
import io.fixprotocol.conga.messages.appl.Side;
import io.fixprotocol.conga.messages.appl.ExecutionReport.Fill;
import io.fixprotocol.conga.messages.appl.MutableExecutionReport.MutableFill;
import io.fixprotocol.conga.sbe.messages.appl.SbeMutableResponseMessageFactory;
import io.fixprotocol.conga.sbe.messages.appl.SbeResponseMessageFactory;

/**
 * @author Don Mendelson
 *
 */
public class SbeResponseTest {
  
  private SbeMutableResponseMessageFactory mutableMessageFactory;
  private SbeResponseMessageFactory messageFactory;
  private ByteBuffer buffer;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    buffer = ByteBuffer.allocate(1024);
    SingleBufferSupplier bufferSupplier = new SingleBufferSupplier(buffer);
    mutableMessageFactory = new SbeMutableResponseMessageFactory(bufferSupplier);
    messageFactory = new SbeResponseMessageFactory();
  }

  @Test
  public void execution() throws MessageException {
    MutableExecutionReport encoder = mutableMessageFactory.getExecutionReport();
    String clOrdId = "CL0001";
    encoder.setClOrdId(clOrdId);
    int cumQty = 3;
    encoder.setCumQty(cumQty);
    String execId = "E00001";
    encoder.setExecId(execId);
    ExecType execType = ExecType.Trade;
    encoder.setExecType(execType);
    int leavesQty = 4;
    encoder.setLeavesQty(leavesQty);
    Side side = Side.Sell;
    encoder.setSide(side);
    String symbol = "SYM1";
    encoder.setSymbol(symbol);
    Instant transactTime = Instant.ofEpochMilli(10000L);
    encoder.setTransactTime(transactTime);
    encoder.setFillCount(1);
    MutableFill fill = encoder.nextFill();
    BigDecimal fillPx = BigDecimal.valueOf(1234, 2);
    fill.setFillPx(fillPx);
    int fillQty = 3;
    fill.setFillQty(fillQty);
    
    buffer.flip();
    
    Message message = messageFactory.wrap(buffer);
    if (message instanceof ExecutionReport) {
      ExecutionReport decoder = (ExecutionReport) message;
      assertEquals(clOrdId, decoder.getClOrdId());
      assertEquals(cumQty, decoder.getCumQty());
      assertEquals(execId, decoder.getExecId());
      assertEquals(execType, decoder.getExecType());
      assertEquals(leavesQty, decoder.getLeavesQty());
      assertEquals(side, decoder.getSide());
      assertEquals(symbol, decoder.getSymbol());
      assertEquals(transactTime, decoder.getTransactTime());
      
      Iterator<? extends Fill> fillIter = decoder.getFills();
      assertTrue(fillIter.hasNext());
      Fill fillDecoder = fillIter.next();
      assertEquals(fillPx, fillDecoder.getFillPx());
      assertEquals(fillQty, fillDecoder.getFillQty());
      assertFalse(fillIter.hasNext());
    } else {
      fail("Wrong message type");
    }
    
    encoder.release();
  }
  
  @Test
  public void reject() throws MessageException {
    MutableOrderCancelReject encoder = mutableMessageFactory.getOrderCancelReject();
    String clOrdId = "CL0001";
    encoder.setClOrdId(clOrdId);
    CxlRejReason cxlRejReason = CxlRejReason.TooLateToCancel;
    encoder.setCxlRejReason(cxlRejReason );
    String orderId = "O0001";
    encoder.setOrderId(orderId);
    OrdStatus ordStatus = OrdStatus.Canceled;
    encoder.setOrdStatus(ordStatus );
    Instant transactTime = Instant.ofEpochMilli(10000L);
    encoder.setTransactTime(transactTime); 
    buffer.flip();
    
    Message message = messageFactory.wrap(buffer);
    if (message instanceof OrderCancelReject) {
      OrderCancelReject decoder = (OrderCancelReject) message;
      assertEquals(clOrdId, decoder.getClOrdId());
      assertEquals(cxlRejReason, decoder.getCxlRejReason());
      assertEquals(orderId, decoder.getOrderId());
      assertEquals(ordStatus, decoder.getOrdStatus());
      assertEquals(transactTime, decoder.getTransactTime());
    } else {
      fail("Wrong message type");
    }
    
    encoder.release();
  }
}
