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

package io.fixprotocol.conga.client;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.fixprotocol.conga.messages.Message;
import io.fixprotocol.conga.messages.MessageListener;
import io.fixprotocol.conga.messages.MutableNewOrderSingle;
import io.fixprotocol.conga.messages.OrdType;
import io.fixprotocol.conga.messages.Side;

/**
 * Requires running Exchange
 * 
 * @author Don Mendelson
 *
 */
public class TraderTest {

  private Trader trader;
  private class TestMessageListener implements MessageListener {

    private int count = 0;
    
    @Override
    public void onMessage(Message message) {
      count++;
      //System.out.println(message.toString());
    }

    public int getCount() {
      return count;
    }

    public void reset() {
      this.count = 0;
    }

    @Override
    public void onError(Throwable error) {
      error.printStackTrace();
      fail();
    }
    
  };
  
  private TestMessageListener listener = new TestMessageListener();
  
  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    listener.reset();
    trader = new Trader("localhost", 8025, "/trade", 30);
    trader.open(listener );
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    if (trader != null) {
      trader.close();
    }
  }

  @Test
  public void selfTrade() throws Exception {
    MutableNewOrderSingle order1 = trader.createOrder();
    order1.setClOrdId("C1");
    order1.setOrderQty(3);
    order1.setOrdType(OrdType.Limit);
    order1.setPrice(new BigDecimal("73.45"));
    order1.setSide(Side.Buy);
    order1.setSymbol("S1");
    order1.setTransactTime(Instant.now());
    long nextSeqNo = trader.send(order1);
    order1.release();
    assertEquals(2, nextSeqNo);
    
    MutableNewOrderSingle order2 = trader.createOrder();
    order2.setClOrdId("C2");
    order2.setOrderQty(3);
    order2.setOrdType(OrdType.Limit);
    order2.setPrice(new BigDecimal("73.45"));
    order2.setSide(Side.Sell);
    order2.setSymbol("S1");
    order2.setTransactTime(Instant.now());
    nextSeqNo = trader.send(order2);
    order2.release();
    assertEquals(3, nextSeqNo);
    
    Thread.sleep(1000);
    
    assertEquals(3, listener.getCount());
    
    //System.out.println(trader.toString());
  }

}
