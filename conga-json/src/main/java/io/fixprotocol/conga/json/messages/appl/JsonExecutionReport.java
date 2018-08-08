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
import java.util.Iterator;

import io.fixprotocol.conga.messages.appl.ExecType;
import io.fixprotocol.conga.messages.appl.ExecutionReport;
import io.fixprotocol.conga.messages.appl.Message;
import io.fixprotocol.conga.messages.appl.OrdStatus;
import io.fixprotocol.conga.messages.appl.Side;

/**
 * @author Don Mendelson
 *
 */
public class JsonExecutionReport implements ExecutionReport, Message {

  private String clOrdId;
  private int cumQty;
  private String execId;
  private ExecType execType;
  private ArrayList<JsonFill> fills = new ArrayList<>();
  private int leavesQty;
  private String orderId;
  private OrdStatus ordStatus;
  private Side side;
  private transient String source;
  private String symbol;
  private Instant transactTime;

  @Override
  public String getClOrdId() {
    return clOrdId;
  }

  @Override
  public int getCumQty() {
    return cumQty;
  }

  @Override
  public String getExecId() {
    return execId;
  }

  @Override
  public ExecType getExecType() {
    return execType;
  }

  @Override
  public Iterator<? extends Fill> getFills() {
    return fills.iterator();
  }

  @Override
  public int getLeavesQty() {
    return leavesQty;
  }

  @Override
  public String getOrderId() {
    return orderId;
  }

  @Override
  public OrdStatus getOrdStatus() {
    return ordStatus;
  }

  @Override
  public Side getSide() {
    return side;
  }

  /**
   * @return the source
   */
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

  /**
   * @param source the source to set
   */
  public void setSource(String source) {
    this.source = source;
  }

}
