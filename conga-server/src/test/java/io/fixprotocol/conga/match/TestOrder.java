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

import java.math.BigDecimal;
import java.time.Instant;

import io.fixprotocol.conga.messages.NewOrderSingle;
import io.fixprotocol.conga.messages.OrdType;
import io.fixprotocol.conga.messages.Side;

class TestOrder implements NewOrderSingle {

  private String clOrdId;
  private int orderQty;
  private OrdType ordType;
  private BigDecimal price;
  private Side side;
  private String symbol;

  private Instant transactTime = Instant.now();

  public TestOrder(String clOrdId, String symbol, Side side, int orderQty, OrdType ordType,
      BigDecimal price) {
    super();
    this.clOrdId = clOrdId;
    this.symbol = symbol;
    this.side = side;
    this.orderQty = orderQty;
    this.ordType = ordType;
    this.price = price;
  }

  @Override
  public String getClOrdId() {
    return clOrdId;
  }

  @Override
  public int getOrderQty() {
    return orderQty;
  }

  @Override
  public OrdType getOrdType() {
    return ordType;
  }

  @Override
  public BigDecimal getPrice() {
    return price;
  }

  @Override
  public Side getSide() {
    return side;
  }

  @Override
  public String getSymbol() {
    return symbol;
  }

  @Override
  public Instant getTransactTime() {
    return transactTime;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("TestOrder [clOrdId=").append(clOrdId).append(", symbol=").append(symbol)
        .append(", side=").append(side).append(", transactTime=").append(transactTime)
        .append(", orderQty=").append(orderQty).append(", ordType=").append(ordType)
        .append(", price=").append(price).append("]");
    return builder.toString();
  }

}
