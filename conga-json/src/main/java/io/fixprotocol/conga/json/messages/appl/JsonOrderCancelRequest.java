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

import java.time.Instant;

import com.google.gson.annotations.SerializedName;

import io.fixprotocol.conga.messages.appl.Message;
import io.fixprotocol.conga.messages.appl.OrderCancelRequest;
import io.fixprotocol.conga.messages.appl.Side;

/**
 * @author Don Mendelson
 *
 */
public class JsonOrderCancelRequest implements OrderCancelRequest, Message {
  
  private String clOrdId;
  private Side side;
  private transient String source;
  private String symbol;
  private Instant transactTime;
  
  @SerializedName("@type")
  private String type = "OrderCancelRequest";

  @Override
  public String getClOrdId() {
    return clOrdId;
  }

  @Override
  public Side getSide() {
    return side;
  }

  public String getSource() {
    return source;
  }
  
  @Override
  public String getSymbol() {
    return symbol;
  }

  @Override
  public Instant getTransactTime() {
    return transactTime;
  }

  public void setSource(String source) {
    this.source = source;
  }

}
