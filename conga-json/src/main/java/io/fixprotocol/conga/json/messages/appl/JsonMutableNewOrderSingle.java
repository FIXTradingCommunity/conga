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

package io.fixprotocol.conga.json.messages.appl;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.json.messages.JsonMutableMessage;
import io.fixprotocol.conga.messages.appl.MutableNewOrderSingle;
import io.fixprotocol.conga.messages.appl.OrdType;
import io.fixprotocol.conga.messages.appl.Side;

/**
 * @author Don Mendelson
 *
 */
public class JsonMutableNewOrderSingle extends JsonMutableMessage implements MutableNewOrderSingle {

  private String clOrdId;
  private int orderQty;
  private OrdType ordType;
  private BigDecimal price;
  private Side side;
  private String symbol;
  private Instant transactTime;
  
  @SerializedName("@type")
  private final String type = "NewOrderSingle";

  /**
   * Constructor
   * @param bufferSupplier supplies a buffer on demand
   */
  public JsonMutableNewOrderSingle(BufferSupplier bufferSupplier) {
    super(bufferSupplier);
  }

  @Override
  public void setClOrdId(String clOrdId) {
    this.clOrdId = clOrdId;
  }

  @Override
  public void setOrderQty(int orderQty) {
    this.orderQty = orderQty;
  }

  @Override
  public void setOrdType(OrdType ordType) {
    this.ordType = ordType;
  }

  @Override
  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  @Override
  public void setSide(Side side) {
    this.side = side;
  }

  @Override
  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  @Override
  public void setTransactTime(Instant transactTime) {
    this.transactTime = transactTime;
  }

}
