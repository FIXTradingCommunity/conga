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
import java.time.Instant;

import io.fixprotocol.conga.messages.appl.NewOrderSingle;
import io.fixprotocol.conga.messages.appl.OrdType;
import io.fixprotocol.conga.messages.appl.Side;

/**
 * State of an order while being matched
 * 
 * @author Don Mendelson
 *
 */
class WorkingOrder implements NewOrderSingle {

  private int cumQty = 0;
  private final Instant entryTime;
  private int leavesQty = 0;
  private final NewOrderSingle order;
  private final String orderId;
  private final String source;

  /**
   * Wraps an incoming order with state information
   * 
   * <p>
   * Key fields for equality are source and ClOrdId
   * 
   * @param order incoming order message
   * @param source order originator
   * @param orderId assigned order ID
   * @param entryTime assigned by the system
   */
  public WorkingOrder(NewOrderSingle order, String source, String orderId, Instant entryTime) {
    this.order = order;
    this.source = source;
    this.entryTime = entryTime;
    this.leavesQty = order.getOrderQty();
    this.orderId = orderId;
  }

  public void close() {
    leavesQty = 0;
  }

  public void execute(int fillQty) {
    cumQty += fillQty;
    leavesQty -= fillQty;
  }

  @Override
  public String getClOrdId() {
    return order.getClOrdId();
  }

  public int getCumQty() {
    return cumQty;
  }

  public Instant getEntryTime() {
    return entryTime;
  }

  public int getLeavesQty() {
    return leavesQty;
  }

  public String getOrderId() {
    return orderId;
  }

  @Override
  public int getOrderQty() {
    return order.getOrderQty();
  }

  @Override
  public OrdType getOrdType() {
    return order.getOrdType();
  }

  @Override
  public BigDecimal getPrice() {
    return order.getPrice();
  }

  @Override
  public Side getSide() {
    return order.getSide();
  }

  @Override
  public String getSymbol() {
    return order.getSymbol();
  }

  @Override
  public Instant getTransactTime() {
    return order.getTransactTime();
  }

  public String getSource() {
    return source;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("WorkingOrder [cumQty=").append(cumQty).append(", entryTime=").append(entryTime)
        .append(", leavesQty=").append(leavesQty).append(", order=").append(order)
        .append(", orderId=").append(orderId).append(", source=").append(source)
        .append(", getPrice()=").append(getPrice()).append("]");
    return builder.toString();
  }

}
