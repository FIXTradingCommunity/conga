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

/**
 * Decorates an order for matching. 
 * 
 * <p>Only price is significant, not entry time. Market orders match any price.
 * 
 * @author Don Mendelson
 *
 */
class MatchingOrderDecorator extends WorkingOrder {

  // BigDecimal doesn't actually have min or max value, but these should be
  // good enough for practical purposes.
  private static final BigDecimal MAX_PRICE = BigDecimal.valueOf(Long.MAX_VALUE);
  private static final BigDecimal MIN_PRICE = BigDecimal.valueOf(Long.MIN_VALUE);
  

  /**
   * Wrap an order with override behavior for matching
   * @param order order to wrap
   * @param userId user that originated order
   * @param orderId assigned order ID
   * @param entryTime assigned entry time
   */
  MatchingOrderDecorator(NewOrderSingle order, String userId) {
    super(order, userId, "None", Instant.MAX);
  }
 
  @Override
  public BigDecimal getPrice() {
    if (OrdType.Market != getOrdType()) {
      return super.getPrice();
    } else if (Side.Buy == getSide()) {
      return MAX_PRICE;
    } else {
      return MIN_PRICE;
    }
  }
}
