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

package io.fixprotocol.conga.messages.appl;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Encodes an order message
 * 
 * @author Don Mendelson
 *
 */
public interface MutableNewOrderSingle extends MutableMessage {

  /**
   * Set client order ID 
   * @param clOrdId client order ID
   */
  void setClOrdId(String clOrdId);

  /**
   * Set order symbol
   * @param symbol instrument identifier
   */
  void setSymbol(String symbol);

  /**
   * Set order side
   * @param side buy or sell
   */
  void setSide(Side side);

  /**
   * Set transaction time
   * @param transactTime UTC timestamp
   */
  void setTransactTime(Instant transactTime);

  /**
   * Set order quantity
   * @param orderQty quantity to buy or sell, e.g. shares or contracts
   */
  void setOrderQty(int orderQty);

  /**
   * Set order type
   * @param ordType the type of this order
   */
  void setOrdType(OrdType ordType);

  /**
   * Set limit price
   * @param price limit price
   */
  void setPrice(BigDecimal price);

}
