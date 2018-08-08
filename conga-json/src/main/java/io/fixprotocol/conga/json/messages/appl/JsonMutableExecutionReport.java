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
import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.annotations.SerializedName;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.json.messages.JsonMutableMessage;
import io.fixprotocol.conga.messages.appl.ExecType;
import io.fixprotocol.conga.messages.appl.MutableExecutionReport;
import io.fixprotocol.conga.messages.appl.OrdStatus;
import io.fixprotocol.conga.messages.appl.Side;

/**
 * @author Don Mendelson
 *
 */
public class JsonMutableExecutionReport extends JsonMutableMessage implements MutableExecutionReport {
  
  @SerializedName("@type")
  private final String type = "ExecutionReport";
  
  private transient String source;
  private String clOrdId;
  private int cumQty;
  private String execId;
  private ExecType execType;
  private int leavesQty;
  private String orderId;
  private OrdStatus ordStatus;
  private Side side;
  private String symbol;
  private Instant transactTime;
  private ArrayList<JsonMutableFill> fills = new ArrayList<>();
  
  /**
   * @param bufferSupplier
   */
  public JsonMutableExecutionReport(BufferSupplier bufferSupplier) {
    super(bufferSupplier);
  }

  @Override
  public void setFillCount(int count) {
    fills.ensureCapacity(count);
  }

  @Override
  public MutableFill nextFill() {
    JsonMutableFill fill = new JsonMutableFill();
    fills.add(fill);
    return fill;
  }

  @Override
  public void setClOrdId(String clOrdId) {
    this.clOrdId = clOrdId;
  }

  @Override
  public void setCumQty(int cumQty) {
    this.cumQty = cumQty;
  }

  @Override
  public void setExecId(String execId) {
    this.execId = execId;
  }

  @Override
  public void setExecType(ExecType execType) {
    this.execType = execType;
  }

  @Override
  public void setLeavesQty(int leavesQty) {
    this.leavesQty = leavesQty;
  }

  @Override
  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  @Override
  public void setOrdStatus(OrdStatus ordStatus) {
    this.ordStatus = ordStatus;
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
