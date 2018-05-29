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

package io.fixprotocol.conga.match;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.fixprotocol.conga.messages.CxlRejReason;
import io.fixprotocol.conga.messages.ExecType;
import io.fixprotocol.conga.messages.Message;
import io.fixprotocol.conga.messages.MutableExecutionReport;
import io.fixprotocol.conga.messages.MutableMessage;
import io.fixprotocol.conga.messages.MutableOrderCancelReject;
import io.fixprotocol.conga.messages.MutableResponseMessageFactory;
import io.fixprotocol.conga.messages.OrdStatus;
import io.fixprotocol.conga.messages.OrdType;
import io.fixprotocol.conga.messages.OrderCancelRequest;
import io.fixprotocol.conga.messages.Side;

/**
 * @author Don Mendelson
 *
 */
public class MatchEngineTest {

  static class TestCancelReject implements MutableOrderCancelReject {

    String clOrdId;
    CxlRejReason cxlRejReason;
    String orderId;
    OrdStatus ordStatus;
    Instant transactTime;

    @Override
    public void setClOrdId(String clOrdId) {
      this.clOrdId = clOrdId;
    }

    @Override
    public void setCxlRejReason(CxlRejReason cxlRejReason) {
      this.cxlRejReason = cxlRejReason;
    }

    @Override
    public void setOrderId(String orderId) {
      this.orderId = orderId;
    }

    @Override
    public void setOrdStatus(OrdStatus ordStatus) {
      this.ordStatus = ordStatus;
    }

    @Override
    public void setTransactTime(Instant time) {
      this.transactTime = time;
    }

    @Override
    public void release() {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void setSource(String source) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public String getSource() {
      // TODO Auto-generated method stub
      return null;
    }

    /* (non-Javadoc)
     * @see io.fixprotocol.conga.messages.Message#toBuffer()
     */
    @Override
    public ByteBuffer toBuffer() {
      // TODO Auto-generated method stub
      return null;
    }


  }
  static class TestExecution implements MutableExecutionReport {

    class TestFill implements MutableFill {

      BigDecimal fillPx;
      int fillQty;

      @Override
      public void setFillPx(BigDecimal fillPx) {
        this.fillPx = fillPx;
      }

      @Override
      public void setFillQty(int fillQty) {
        this.fillQty = fillQty;
      }
    }

    private int fillCount = 0;
    private int fillIndex = -1;
    private ArrayList<TestFill> fills = new ArrayList<>();
    
    String clOrdId;
    int cumQty;
    String execId;
    ExecType execType;
    int leavesQty;
    String orderId;

    OrdStatus ordStatus;
    Side side;
    String symbol;
    Instant transactTime;

    @Override
    public MutableFill nextFill() throws NoSuchElementException {
      try {
        ++fillIndex;
        return fills.get(fillIndex);
      } catch (IndexOutOfBoundsException e) {
        throw new NoSuchElementException();
      }
    }

    @Override
    public void setClOrdId(String clOrdId) {
      this.clOrdId = clOrdId;
    }

    @Override
    public void setCumQty(int cumQty) {
      this.cumQty = cumQty;
    }

    @Override
    public void setExecId(String execId) {
      this.execId = execId;
    }

    @Override
    public void setExecType(ExecType execType) {
      this.execType = execType;
    }

    @Override
    public void setFillCount(int count) {
      this.fillCount = count;
      this.fillIndex = -1;
      for (int i = 0; i < count; i++) {
        this.fills.add(new TestFill());
      }
    }

    @Override
    public void setLeavesQty(int leavesQty) {
      this.leavesQty = leavesQty;
    }

    @Override
    public void setOrderId(String orderId) {
      this.orderId = orderId;
    }

    @Override
    public void setOrdStatus(OrdStatus ordStatus) {
      this.ordStatus = ordStatus;
    }

    @Override
    public void setSide(Side side) {
      this.side = side;
    }

    @Override
    public void setSymbol(String symbol) {
      this.symbol = symbol;
    }


    @Override
    public void setTransactTime(Instant time) {
      this.transactTime = time;
    }

    @Override
    public void release() {
      this.fillCount = 0;
      this.fillIndex = -1;
      this.fills.clear();  
    }

    @Override
    public void setSource(String source) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public String getSource() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public ByteBuffer toBuffer() {
      // TODO Auto-generated method stub
      return null;
    }

  }

  private static MutableResponseMessageFactory messageFactory;

  @BeforeClass
  public static void setUpOnce() {
    messageFactory = new MutableResponseMessageFactory() {

      @Override
      public MutableExecutionReport getExecutionReport() {
        return new TestExecution();
      }

      @Override
      public MutableOrderCancelReject getOrderCancelReject() {
        return new TestCancelReject();
      }

    };
  }

  private MatchEngine engine;
  private String symbol = "SYM1";
  private String userId = "USER1";

  @Test
  public void cancelOrderUnknown() {
    TestOrder order =
        new TestOrder("C1", symbol, Side.Sell, 7, OrdType.Limit, new BigDecimal("12.95"));
    List<MutableMessage> responses = engine.onOrder(userId, order);
    assertEquals(1, responses.size());

    OrderCancelRequest cancel = new TestCancelRequest("C2", symbol, Side.Buy, Instant.now());
    List<MutableMessage> responses2 = engine.onCancelRequest(userId, cancel);
    assertEquals(1, responses2.size());
    Message response = responses2.get(0);
    assertTrue(response instanceof TestCancelReject);
    TestCancelReject reject = (TestCancelReject) response;
    assertEquals(OrdStatus.Rejected, reject.ordStatus);
    assertEquals(CxlRejReason.UnknownOrder, reject.cxlRejReason);
  }

  @Test
  public void orderCancel() {
    TestOrder order =
        new TestOrder("C1", symbol, Side.Sell, 7, OrdType.Limit, new BigDecimal("12.95"));
    List<MutableMessage> responses = engine.onOrder(userId, order);
    assertEquals(1, responses.size());
    Message response = responses.get(0);
    assertTrue(response instanceof TestExecution);
    TestExecution execution = (TestExecution) response;
    assertEquals(OrdStatus.New, execution.ordStatus);
    assertEquals(7, execution.leavesQty);
    assertEquals(0, execution.cumQty);

    OrderCancelRequest cancel = new TestCancelRequest("C1", symbol, Side.Sell, Instant.now());
    List<MutableMessage> responses2 = engine.onCancelRequest(userId, cancel);
    assertEquals(1, responses2.size());
    Message response2 = responses2.get(0);
    assertTrue(response2 instanceof TestExecution);
    TestExecution execution2 = (TestExecution) response2;
    assertEquals(OrdStatus.Canceled, execution2.ordStatus);
    assertEquals(0, execution2.leavesQty);
    assertEquals(0, execution2.cumQty);
  }

  @Test
  public void orderFill() {
    TestOrder order =
        new TestOrder("C1", symbol, Side.Sell, 7, OrdType.Limit, new BigDecimal("12.95"));
    List<MutableMessage> responses = engine.onOrder(userId, order);
    assertEquals(1, responses.size());
    Message response = responses.get(0);
    assertTrue(response instanceof TestExecution);
    TestExecution execution = (TestExecution) response;
    assertEquals(OrdStatus.New, execution.ordStatus);
    assertEquals(7, execution.leavesQty);
    assertEquals(0, execution.cumQty);

    TestOrder order2 =
        new TestOrder("C2", symbol, Side.Buy, 7, OrdType.Limit, new BigDecimal("12.95"));
    List<MutableMessage> responses2 = engine.onOrder(userId, order2);
    assertEquals(2, responses2.size());
    for (Message aResponse : responses2) {
      assertTrue(aResponse instanceof TestExecution);
      TestExecution anExecution = (TestExecution) aResponse;
      assertEquals(OrdStatus.Filled, anExecution.ordStatus);
      assertEquals(0, anExecution.leavesQty);
      assertEquals(7, anExecution.cumQty);
    }
    
    OrderBook orderBook = engine.getOrderBooks().get(symbol);
    assertTrue(orderBook.getBids().isEmpty());
    assertTrue(orderBook.getOffers().isEmpty());
  }

  @Test
  public void orderPartialFill() {
    TestOrder order1 =
        new TestOrder("C1", symbol, Side.Sell, 3, OrdType.Limit, new BigDecimal("12.94"));
    List<MutableMessage> responses = engine.onOrder(userId, order1);
    assertEquals(1, responses.size());

    TestOrder order2 =
        new TestOrder("C2", symbol, Side.Sell, 2, OrdType.Limit, new BigDecimal("12.95"));
    List<MutableMessage> responses2 = engine.onOrder(userId, order2);
    assertEquals(1, responses2.size());

    TestOrder order3 =
        new TestOrder("C3", symbol, Side.Sell, 3, OrdType.Limit, new BigDecimal("12.96"));
    List<MutableMessage> responses3 = engine.onOrder(userId, order3);
    assertEquals(1, responses3.size());

    TestOrder order4 =
        new TestOrder("C4", symbol, Side.Buy, 7, OrdType.Limit, new BigDecimal("12.95"));
    List<MutableMessage> responses4 = engine.onOrder(userId, order4);
    assertEquals(3, responses4.size());
    for (Message aResponse : responses4) {
      assertTrue(aResponse instanceof TestExecution);
      TestExecution anExecution = (TestExecution) aResponse;
      switch (anExecution.clOrdId) {
        case "C1":
          assertEquals(OrdStatus.Filled, anExecution.ordStatus);
          assertEquals(0, anExecution.leavesQty);
          assertEquals(3, anExecution.cumQty);
          break;
        case "C2":
          assertEquals(OrdStatus.Filled, anExecution.ordStatus);
          assertEquals(0, anExecution.leavesQty);
          assertEquals(2, anExecution.cumQty);
          break;
        case "C4":
          assertEquals(OrdStatus.PartiallyFilled, anExecution.ordStatus);
          assertEquals(2, anExecution.leavesQty);
          assertEquals(5, anExecution.cumQty);
          break;
        default:
          fail("bad match");
      }
    }
  }

  @Test
  public void orderTimePriorityBuy() {
    TestOrder order1 =
        new TestOrder("C1", symbol, Side.Sell, 3, OrdType.Limit, new BigDecimal("12.95"));
    List<MutableMessage> responses = engine.onOrder(userId, order1);
    assertEquals(1, responses.size());

    TestOrder order2 =
        new TestOrder("C2", symbol, Side.Sell, 3, OrdType.Limit, new BigDecimal("12.95"));
    List<MutableMessage> responses2 = engine.onOrder(userId, order2);
    assertEquals(1, responses2.size());

    TestOrder order3 =
        new TestOrder("C3", symbol, Side.Buy, 3, OrdType.Limit, new BigDecimal("12.95"));
    List<MutableMessage> responses3 = engine.onOrder(userId, order3);
    assertEquals(2, responses3.size());
    for (Message aResponse : responses3) {
      assertTrue(aResponse instanceof TestExecution);
      TestExecution anExecution = (TestExecution) aResponse;
      switch (anExecution.clOrdId) {
        case "C1":
          assertEquals(OrdStatus.Filled, anExecution.ordStatus);
          assertEquals(0, anExecution.leavesQty);
          assertEquals(3, anExecution.cumQty);
          break;
        case "C3":
          assertEquals(OrdStatus.Filled, anExecution.ordStatus);
          assertEquals(0, anExecution.leavesQty);
          assertEquals(3, anExecution.cumQty);
          break;
        default:
          fail("bad match");
      }
    }
  }

  @Test
  public void orderTimePrioritySell() {
    TestOrder order1 =
        new TestOrder("C1", symbol, Side.Buy, 3, OrdType.Limit, new BigDecimal("12.95"));
    List<MutableMessage> responses = engine.onOrder(userId, order1);
    assertEquals(1, responses.size());

    TestOrder order2 =
        new TestOrder("C2", symbol, Side.Buy, 3, OrdType.Limit, new BigDecimal("12.95"));
    List<MutableMessage> responses2 = engine.onOrder(userId, order2);
    assertEquals(1, responses2.size());

    TestOrder order3 =
        new TestOrder("C3", symbol, Side.Sell, 3, OrdType.Limit, new BigDecimal("12.95"));
    List<MutableMessage> responses3 = engine.onOrder(userId, order3);
    assertEquals(2, responses3.size());
    for (Message aResponse : responses3) {
      assertTrue(aResponse instanceof TestExecution);
      TestExecution anExecution = (TestExecution) aResponse;
      switch (anExecution.clOrdId) {
        case "C1":
          assertEquals(OrdStatus.Filled, anExecution.ordStatus);
          assertEquals(0, anExecution.leavesQty);
          assertEquals(3, anExecution.cumQty);
          break;
        case "C3":
          assertEquals(OrdStatus.Filled, anExecution.ordStatus);
          assertEquals(0, anExecution.leavesQty);
          assertEquals(3, anExecution.cumQty);
          break;
        default:
          fail("bad match");
      }
    }
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    engine = new MatchEngine(messageFactory, new TestClock());
  }


}
