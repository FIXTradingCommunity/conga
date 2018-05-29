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

import java.util.*;

import io.fixprotocol.conga.messages.Side;

/**
 * Limit order book
 * 
 * <p>
 * Not thread-safe; it is assumed that order matching is single-threaded.
 * 
 * @author Don Mendelson
 *
 */
public class OrderBook {

  private final NavigableSet<WorkingOrder> bids;
  private final NavigableSet<WorkingOrder> offers;

  /**
   * Constructor
   * 
   * By default, orders are sorted by price and entry time
   */
  public OrderBook() {
    this(
        // Descending order by price, ascending order by entry time
        ((Comparator<WorkingOrder>) (o1, o2) -> o2.getPrice().compareTo(o1.getPrice())).thenComparing(WorkingOrder::getEntryTime),
        // Ascending order by price, ascending order by entry time
        Comparator.comparing(WorkingOrder::getPrice).thenComparing(WorkingOrder::getEntryTime));
  }


  /**
   * Constructor 
   * @param bidComparator prioritizes bids
   * @param offerComparator prioritizes offers
   */
  public OrderBook(Comparator<WorkingOrder> bidComparator,
      Comparator<WorkingOrder> offerComparator) {
    this.bids = new TreeSet<WorkingOrder>(bidComparator);
    this.offers = new TreeSet<WorkingOrder>(offerComparator);
  }

  /**
   * Adds an order to this OrderBook
   * 
   * @param order and order to book
   */
  public void addOrder(WorkingOrder order) {
    switch (order.getSide()) {
      case Buy:
        bids.add(order);
        break;
      case Sell:
        offers.add(order);
        break;
      default:
        throw new IllegalArgumentException("Invalid side");
    }
  }

  /**
   * Removes all orders from this OrderBook
   */
  public void clear() {
    bids.clear();
    offers.clear();
  }


  /**
   * Returns potential matches on the opposite side of the book
   * 
   * @param order an order to match
   * @return a Set of potential matches, based on price/time priority
   */
  public SortedSet<WorkingOrder> findMatches(WorkingOrder order) {
    // Matching only compares price on the opposite side, not entry time
    MatchingOrderDecorator compare = new MatchingOrderDecorator(order, order.getSource());
    SortedSet<WorkingOrder> matches = null;
    switch (order.getSide()) {
      case Buy:
        matches = offers.headSet(compare, true);
        break;
      case Sell:
        matches = bids.headSet(compare, true);
        break;
      default:
        throw new IllegalArgumentException("Invalid side");
    }
    return matches;
  }

  /**
   * Finds the state of an order in this OrderBook
   * 
   * @param side Buy or Sell
   * @param clOrdId order identifier
   * @param userId user identifier
   * @return Returns the WorkingOrder with current state or {@code null} if not found
   */
  public WorkingOrder findOrder(Side side, String clOrdId, String userId) {
    SortedSet<WorkingOrder> orders = null;
    switch (side) {
      case Buy:
        orders = bids;
        break;
      case Sell:
        orders = offers;
        break;
      default:
        throw new IllegalArgumentException("Invalid side");
    }
    for (WorkingOrder found : orders) {
      if (found.getClOrdId().equals(clOrdId) && found.getSource().equals(userId)) {
        return found;
      }
    }
    return null;
  }

  /**
   * Finds the state of an order in this OrderBook
   * 
   * @param order order object with key fields to match
   * @return Returns the WorkingOrder with current state or {@code null} if not found
   */
  public WorkingOrder findOrder(WorkingOrder order) {
    SortedSet<WorkingOrder> orders = null;
    switch (order.getSide()) {
      case Buy:
        orders = bids.tailSet(order, true);
        break;
      case Sell:
        orders = offers.tailSet(order, true);
        break;
      default:
        throw new IllegalArgumentException("Invalid side");
    }
    for (WorkingOrder found : orders) {
      if (found.equals(order)) {
        return found;
      }
    }
    return null;
  }

  /**
   * Removes an order from this OrderBook
   * 
   * @param order an order with matching key fields
   * @return Returns the removed order or {@code null} if it is not found
   */
  public WorkingOrder removeOrder(Side side, String clOrdId, String userId) {
    SortedSet<WorkingOrder> orders = null;
    switch (side) {
      case Buy:
        orders = bids;
        break;
      case Sell:
        orders = offers;
        break;
      default:
        throw new IllegalArgumentException("Invalid side");
    }
    for (WorkingOrder found : orders) {
      if (found.getClOrdId().equals(clOrdId) && found.getSource().equals(userId)) {
        orders.remove(found);
        return found;
      }
    }
    return null;
  }

  /**
   * Removes an order from this OrderBook
   * 
   * @param order an order with matching key fields
   * @return Returns the removed order or {@code null} if it is not found
   */
  public WorkingOrder removeOrder(WorkingOrder order) {
    SortedSet<WorkingOrder> orders = null;
    switch (order.getSide()) {
      case Buy:
        orders = bids.tailSet(order, true);
        break;
      case Sell:
        orders = offers.tailSet(order, true);
        break;
      default:
        throw new IllegalArgumentException("Invalid side");
    }
    for (WorkingOrder found : orders) {
      if (found.equals(order)) {
        orders.remove(found);
        return found;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("OrderBook [");
    if (null != bids)
      builder.append("bids=").append(bids).append(", ");
    if (null != offers)
      builder.append("offers=").append(offers);
    builder.append("]");
    return builder.toString();
  }

  SortedSet<WorkingOrder> getBids() {
    return Collections.unmodifiableSortedSet(bids);
  }

  SortedSet<WorkingOrder> getOffers() {
    return Collections.unmodifiableSortedSet(offers);
  }
}
