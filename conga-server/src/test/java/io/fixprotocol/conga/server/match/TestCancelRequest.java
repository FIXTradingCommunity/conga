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

import java.time.Instant;

import io.fixprotocol.conga.messages.appl.OrderCancelRequest;
import io.fixprotocol.conga.messages.appl.Side;

/**
 * @author Don Mendelson
 *
 */
public class TestCancelRequest implements OrderCancelRequest {

  private final String clOrdId;
  private final Side side;
  private final String symbol;
  private final Instant transactTime;

  public TestCancelRequest(String clOrdId, String symbol, Side side, Instant transactTime) {
    super();
    this.clOrdId = clOrdId;
    this.symbol = symbol;
    this.side = side;
    this.transactTime = transactTime;
  }

  @Override
  public String getClOrdId() {
    return clOrdId;
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
    builder.append("TestCancelRequest [clOrdId=").append(clOrdId).append(", symbol=").append(symbol)
        .append(", side=").append(side).append(", transactTime=").append(transactTime).append("]");
    return builder.toString();
  }

}
