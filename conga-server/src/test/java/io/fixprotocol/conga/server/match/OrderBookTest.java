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

package io.fixprotocol.conga.server.match;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.SortedSet;

import org.junit.Before;
import org.junit.Test;

import io.fixprotocol.conga.messages.appl.OrdType;
import io.fixprotocol.conga.messages.appl.Side;
import io.fixprotocol.conga.server.match.OrderBook;
import io.fixprotocol.conga.server.match.WorkingOrder;

/**
 * @author Don Mendelson
 *
 */
public class OrderBookTest {

  private OrderBook orderBook;
  private String userId;
  private String symbol;
  private TestClock clock = new TestClock();
  
  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    userId = "USER1";
    symbol = "SYM01";
    orderBook = new OrderBook();
  }

  @Test
  public void testAddOrder() {
    Instant entryTime = clock.instant();
    final WorkingOrder order = new WorkingOrder(
        new TestOrder("ClOrdId1", symbol, Side.Buy, 7, OrdType.Limit, new BigDecimal("12.34")),
        userId, "Order1", entryTime);
    orderBook.addOrder(order);
    WorkingOrder order2 = orderBook.findOrder(order);
    assertEquals(order2, order);
    assertNotNull(orderBook.removeOrder(order));
    assertNull(orderBook.findOrder(order));
  }
  
  @Test
  public void testFindMatchingBids() {
    
    for (int i = 1; i <= 10; i++) {
      Instant entryTime = clock.instant();
      final WorkingOrder order = new WorkingOrder(
          new TestOrder("ClOrdId" + i, symbol, Side.Sell, i, OrdType.Limit, BigDecimal.valueOf(i)),
          userId, "Order" + i, entryTime);
      orderBook.addOrder(order);
    }

    Instant entryTime = clock.instant();
    final WorkingOrder offer = new WorkingOrder(
        new TestOrder("ClOrdId-Bid", symbol, Side.Buy, 5, OrdType.Limit, BigDecimal.valueOf(5)),
        userId, "Bid1", entryTime);
    SortedSet<WorkingOrder> matches = orderBook.findMatches(offer);
    // Should match price 1..5
    assertEquals(5, matches.size());
  }

  @Test
  public void testFindMatchingOffers() {
    for (int i = 1; i <= 10; i++) {
      Instant entryTime = clock.instant();
      final WorkingOrder order = new WorkingOrder(
          new TestOrder("ClOrdId" + i, symbol, Side.Buy, i, OrdType.Limit, BigDecimal.valueOf(i)),
          userId, "Order" + i, entryTime);
      orderBook.addOrder(order);
    }

    Instant entryTime = clock.instant();
    final WorkingOrder offer = new WorkingOrder(
        new TestOrder("ClOrdId-Offer", symbol, Side.Sell, 5, OrdType.Limit, BigDecimal.valueOf(5)),
        userId, "Offer1", entryTime);
    SortedSet<WorkingOrder> matches = orderBook.findMatches(offer);
    // Should match price 5..10
    assertEquals(6, matches.size());
  }
  
  @Test
  public void findMatches() {
    Instant entryTime1 = clock.instant();
    final WorkingOrder order1 = new WorkingOrder(
        new TestOrder("C1", symbol, Side.Sell, 3, OrdType.Limit, new BigDecimal("12.95")), userId,
        "Order" + 1, entryTime1);
    orderBook.addOrder(order1);

    Instant entryTime2 = clock.instant();
    final WorkingOrder order2 = new WorkingOrder(
        new TestOrder("C2", symbol, Side.Sell, 2, OrdType.Limit, new BigDecimal("12.95")), userId,
        "Order" + 2, entryTime2);
    orderBook.addOrder(order2);

    Instant entryTime3 = clock.instant();
    final WorkingOrder order3 = new WorkingOrder(
        new TestOrder("C3", symbol, Side.Buy, 7, OrdType.Limit, new BigDecimal("12.95")), userId,
        "Order" + 3, entryTime3);

    SortedSet<WorkingOrder> matches = orderBook.findMatches(order3);
    assertEquals(2, matches.size());
  }

}
