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
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.fixprotocol.conga.messages.appl.ApplicationMessageConsumer;
import io.fixprotocol.conga.messages.appl.Message;
import io.fixprotocol.conga.messages.appl.MutableNewOrderSingle;
import io.fixprotocol.conga.messages.appl.OrdType;
import io.fixprotocol.conga.messages.appl.Side;
import io.fixprotocol.conga.session.SessionEvent;

/**
 * Requires running Exchange
 * 
 * @author Don Mendelson
 *
 */
@Ignore
public class TraderTest {

  private class TestErrorHandler implements Consumer<Throwable> {

    @Override
    public void accept(Throwable error) {
      error.printStackTrace();
      fail();
    }
  }

  private class TestEventSubscriber implements Subscriber<SessionEvent> {

    private Subscription subscription;

    @Override
    public void onComplete() {
      System.out.println("Session events complete");
    }

    @Override
    public void onError(Throwable throwable) {
      errorHandler.accept(throwable);
    }

    @Override
    public void onNext(SessionEvent item) {
      switch (item.getState()) {
        case ESTABLISHED:
          System.out.println("Session established");
          synchronized(eventLock) {
            eventLock.notify();
          }
          break;
        case NEGOTIATED:
          System.out.println("Session negotiated");
          break;
        case FINALIZED:
          System.out.println("Session finalized");
          break;
        case NOT_ESTABLISHED:
          System.out.println("Session transport unbound");
          break;
        case NOT_NEGOTIATED:
          System.out.println("Session initialized");
          break;
        case FINALIZE_REQUESTED:
          System.out.println("Session finalizing");
          break;
      }
      subscription.request(1);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
    }

    void cancelEventSubscription() {
      if (subscription != null) {
        subscription.cancel();
      }
    }

  };

  private class TestMessageListener implements ApplicationMessageConsumer {

    private int count = 0;

    @Override
    public void accept(String source, Message message, long seqNo) {
      count++;
      // System.out.println(message.toString());
    }

    public int getCount() {
      return count;
    }

    public void reset() {
      this.count = 0;
    }


  };


  private final TestErrorHandler errorHandler = new TestErrorHandler();
  private final TestEventSubscriber eventSubscriber = new TestEventSubscriber();
  private final TestMessageListener listener = new TestMessageListener();
  private Trader trader;
  private final Object eventLock = new Object();

  @Test
  public void quiescent() throws Exception {
    Thread.sleep(10000);
    // System.out.println(trader.toString());
    trader.suspend();
    Thread.sleep(2000);
    trader.open();
    Thread.sleep(2000);
  }

  @Test
  public void selfTrade() throws Exception {
    // System.out.println(trader.toString());
    MutableNewOrderSingle order1 = trader.createOrder();
    order1.setClOrdId("C1");
    order1.setOrderQty(3);
    order1.setOrdType(OrdType.Limit);
    order1.setPrice(new BigDecimal("73.45"));
    order1.setSide(Side.Buy);
    order1.setSymbol("S1");
    order1.setTransactTime(Instant.now());
    long seqNo = trader.send(order1);
    order1.release();
    assertEquals(1, seqNo);

    MutableNewOrderSingle order2 = trader.createOrder();
    order2.setClOrdId("C2");
    order2.setOrderQty(3);
    order2.setOrdType(OrdType.Limit);
    order2.setPrice(new BigDecimal("73.45"));
    order2.setSide(Side.Sell);
    order2.setSymbol("S1");
    order2.setTransactTime(Instant.now());
    seqNo = trader.send(order2);
    order2.release();
    assertEquals(2, seqNo);

    Thread.sleep(1000);

    // System.out.println(trader.toString());
    assertEquals(3, listener.getCount());
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    listener.reset();

    final String encoding = "JSON";
    trader = Trader.builder().remoteHost("localhost").remotePort(8025).apiPath("/trade").timeoutSeconds(2)
        .messageListener(listener).errorListener(errorHandler).encoding(encoding)
        .sessionEventSubscriber(eventSubscriber).build();
    trader.open();
    synchronized(eventLock) {
      eventLock.wait(2000L);
    }
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

}
