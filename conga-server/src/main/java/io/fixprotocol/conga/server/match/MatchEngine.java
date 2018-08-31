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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import io.fixprotocol.conga.messages.appl.CxlRejReason;
import io.fixprotocol.conga.messages.appl.ExecType;
import io.fixprotocol.conga.messages.appl.MutableExecutionReport;
import io.fixprotocol.conga.messages.appl.MutableMessage;
import io.fixprotocol.conga.messages.appl.MutableOrderCancelReject;
import io.fixprotocol.conga.messages.appl.MutableResponseMessageFactory;
import io.fixprotocol.conga.messages.appl.NewOrderSingle;
import io.fixprotocol.conga.messages.appl.OrdStatus;
import io.fixprotocol.conga.messages.appl.OrdType;
import io.fixprotocol.conga.messages.appl.OrderCancelRequest;
import io.fixprotocol.conga.messages.appl.MutableExecutionReport.MutableFill;

/**
 * Matches buy and sell orders
 * 
 * <p>
 * This implementation is only for demonstration of session and encoding layers. Limitations at the
 * application layer:
 * <ul>
 * <li>Only new orders and cancel requests supported; no cancel/replace
 * <li>Only market and limit order types.
 * <li>No good-thru orders or market persistence. Market orders are immediate-or-cancel while limit
 * orders may be booked if not immediately matched. Limit orders have price/time priority.
 * <li>No market phases or schedule; only continuous trading
 * <li>No self-match protection
 * <li>No permission system; test users are assumed to be authorized. User IDs may be transient.
 * <li>There is no pre-configured symbol list; order books are created on demand
 * </ul>
 * Not thread-safe; assumes that matching occurs on a single thread.
 * 
 * @author Don Mendelson
 *
 */
public class MatchEngine {

  private final Clock clock;
  private int executionSequence = 0;
  private final Map<String, OrderBook> orderBooks = new HashMap<>();
  private int orderSequence = 0;
  private final MutableResponseMessageFactory responsMessageFactory;

  /**
   * Constructor
   * 
   * <p>
   * Defaults to system clock with UTC zone
   * 
   * @param responsMessageFactory generates messages for responses
   */
  public MatchEngine(MutableResponseMessageFactory responsMessageFactory) {
    this.responsMessageFactory = responsMessageFactory;
    this.clock = Clock.system(ZoneId.of("Z"));
  }

  /**
   * Constructor
   * 
   * @param messageFactory generates messages for responses
   * @param clock time provider for testing
   */
  public MatchEngine(MutableResponseMessageFactory messageFactory, Clock clock) {
    this.responsMessageFactory = messageFactory;
    this.clock = clock;
  }

  /**
   * Update order book with order cancel request and return response messages
   * 
   * @param source originator of the cancel request
   * @param cancel cancel request
   * @return a list of responses that contains either execution report if the request was successful
   *         or a cancel reject if the order was not in the order book
   */
  public List<MutableMessage> onCancelRequest(String source, OrderCancelRequest cancel) {
    List<MutableMessage> responses = new ArrayList<>();
    var orderBook = orderBooks.get(cancel.getSymbol());
    boolean found = false;
    if (null != orderBook) {
      var order = orderBook.removeOrder(cancel.getSide(), cancel.getClOrdId(), source);
      if (null != order) {
        order.close();
        MutableExecutionReport executionReport = populateExecutionReportCanceled(source, order);
        responses.add(executionReport);
        found = true;
      }
    }
    if (!found) {
      final MutableOrderCancelReject cancelReject =
          populateCancelRejectUnknownOrder(source, cancel);
      responses.add(cancelReject);
    }
    return responses;
  }

  /**
   * Match buy and sell orders, given a new order
   * 
   * <p>
   * First, this MatchEngine attempts to match the new order with orders resting in the book. Zero
   * or more matches may occur until the new order is filled. Resting orders are considered in price
   * and time priority.
   * <p>
   * If the order is not a market order, which is considered immediate-or-cancel, and leaves
   * quantity is greater than zero after all matches, the new order is entered into the book.
   * 
   * @param source originator of the new order
   * @param order an order to match
   * @return a list of responses possibly containing one or more executions when orders were
   *         matched. If no matches occurred, then one execution report is returned for the new
   *         order.
   */
  public List<MutableMessage> onOrder(String source, NewOrderSingle order) {
    List<MutableMessage> responses = new ArrayList<>();
    var orderBook = orderBooks.get(order.getSymbol());
    if (null == orderBook) {
      orderBook = new OrderBook();
      orderBooks.put(order.getSymbol(), orderBook);
    }
    WorkingOrder workingOrder = new WorkingOrder(order, source, getOrderId(), clock.instant());
    SortedSet<WorkingOrder> possibleMatches = orderBook.findMatches(workingOrder);
    List<Integer> fillQtys = new ArrayList<>();
    List<BigDecimal> fillPxs = new ArrayList<>();
    int fillIndex = 0;
    Iterator<WorkingOrder> matchesIter = possibleMatches.iterator();
    while (matchesIter.hasNext()) {
      WorkingOrder possibleMatch = matchesIter.next();
      final int fillQty = Math.min(workingOrder.getLeavesQty(), possibleMatch.getLeavesQty());
      fillQtys.add(fillIndex, fillQty);
      possibleMatch.execute(fillQtys.get(fillIndex));
      workingOrder.execute(fillQtys.get(fillIndex));
      fillPxs.add(fillIndex, possibleMatch.getPrice());

      OrdStatus ordStatus =
          (possibleMatch.getLeavesQty() == 0) ? OrdStatus.Filled : OrdStatus.PartiallyFilled;
      MutableExecutionReport executionReport =
          populateExecutionReportTrade(possibleMatch, fillQtys.subList(fillIndex, fillIndex + 1),
              fillPxs.subList(fillIndex, fillIndex + 1), ordStatus);
      responses.add(executionReport);
      if (possibleMatch.getLeavesQty() == 0) {
        matchesIter.remove();
      }
      fillIndex++;

      if (workingOrder.getLeavesQty() == 0) {
        break;
      }
    }

    if ((workingOrder.getLeavesQty() > 0) && (workingOrder.getOrdType() != OrdType.Market)) {
      orderBook.addOrder(workingOrder);
    }
    if ((workingOrder.getLeavesQty() > 0) && (workingOrder.getOrdType() == OrdType.Market)) {
      workingOrder.close();
      MutableExecutionReport executionReport =
          populateExecutionReportTrade(workingOrder, fillQtys, fillPxs, OrdStatus.Canceled);
      responses.add(executionReport);
    } else if (workingOrder.getCumQty() == 0) {
      MutableExecutionReport executionReport = populateExecutionReportAccepted(workingOrder);
      responses.add(executionReport);
    } else {
      OrdStatus ordStatus =
          (workingOrder.getLeavesQty() == 0) ? OrdStatus.Filled : OrdStatus.PartiallyFilled;
      MutableExecutionReport executionReport =
          populateExecutionReportTrade(workingOrder, fillQtys, fillPxs, ordStatus);
      responses.add(executionReport);
    }
    return responses;
  }

  private String getExecId() {
    return "E" + ++executionSequence;
  }

  private String getOrderId() {
    return "O" + ++orderSequence;
  }

  private MutableOrderCancelReject populateCancelRejectUnknownOrder(String source,
      OrderCancelRequest cancel) {
    final MutableOrderCancelReject cancelReject = responsMessageFactory.getOrderCancelReject();
    cancelReject.setClOrdId(cancel.getClOrdId());
    cancelReject.setCxlRejReason(CxlRejReason.UnknownOrder);
    cancelReject.setOrderId("None");
    cancelReject.setOrdStatus(OrdStatus.Rejected);
    cancelReject.setSource(source);
    cancelReject.setTransactTime(clock.instant());
    return cancelReject;
  }

  private MutableExecutionReport populateExecutionReportAccepted(WorkingOrder workingOrder) {
    MutableExecutionReport executionReport = responsMessageFactory.getExecutionReport();
    executionReport.setClOrdId(workingOrder.getClOrdId());
    executionReport.setCumQty(workingOrder.getCumQty());
    executionReport.setExecId(getExecId());
    executionReport.setExecType(ExecType.New);
    executionReport.setLeavesQty(workingOrder.getLeavesQty());
    executionReport.setOrderId(workingOrder.getOrderId());
    executionReport.setOrdStatus(OrdStatus.New);
    executionReport.setSide(workingOrder.getSide());
    executionReport.setSymbol(workingOrder.getSymbol());
    executionReport.setSource(workingOrder.getSource());
    executionReport.setTransactTime(clock.instant());
    executionReport.setFillCount(0);
    return executionReport;
  }

  private MutableExecutionReport populateExecutionReportCanceled(String source,
      WorkingOrder order) {
    MutableExecutionReport executionReport = responsMessageFactory.getExecutionReport();
    executionReport.setClOrdId(order.getClOrdId());
    executionReport.setCumQty(order.getCumQty());
    executionReport.setExecId(getExecId());
    executionReport.setExecType(ExecType.Canceled);
    executionReport.setLeavesQty(order.getLeavesQty());
    executionReport.setOrderId(order.getOrderId());
    executionReport.setOrdStatus(OrdStatus.Canceled);
    executionReport.setSide(order.getSide());
    executionReport.setSymbol(order.getSymbol());
    executionReport.setSource(source);
    executionReport.setTransactTime(clock.instant());
    return executionReport;
  }

  private MutableExecutionReport populateExecutionReportTrade(WorkingOrder workingOrder,
      List<Integer> fillQtys, List<BigDecimal> fillPxs, OrdStatus ordStatus) {
    MutableExecutionReport executionReport = responsMessageFactory.getExecutionReport();
    executionReport.setClOrdId(workingOrder.getClOrdId());
    executionReport.setCumQty(workingOrder.getCumQty());
    executionReport.setExecId(getExecId());
    executionReport.setExecType(ExecType.Trade);
    executionReport.setLeavesQty(workingOrder.getLeavesQty());
    executionReport.setOrderId(workingOrder.getOrderId());
    executionReport.setOrdStatus(ordStatus);
    executionReport.setSide(workingOrder.getSide());
    executionReport.setSymbol(workingOrder.getSymbol());
    executionReport.setSource(workingOrder.getSource());
    executionReport.setTransactTime(clock.instant());
    executionReport.setFillCount(fillQtys.size());
    for (int i = 0; i < fillQtys.size(); i++) {
      MutableFill fill = executionReport.nextFill();
      fill.setFillPx(fillPxs.get(i));
      fill.setFillQty(fillQtys.get(i));
    }
    return executionReport;
  }

  Map<String, OrderBook> getOrderBooks() {
    return Collections.unmodifiableMap(orderBooks);
  }

}
