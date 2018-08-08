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

import java.nio.ByteBuffer;
import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.BufferSupplier.BufferSupply;
import io.fixprotocol.conga.json.messages.JsonMutableMessage;
import io.fixprotocol.conga.messages.appl.CxlRejReason;
import io.fixprotocol.conga.messages.appl.MutableOrderCancelReject;
import io.fixprotocol.conga.messages.appl.OrdStatus;

/**
 * @author Don Mendelson
 *
 */
public class JsonMutableOrderCancelReject extends JsonMutableMessage implements MutableOrderCancelReject {

  private String clOrdId;
  private CxlRejReason cxlRejReason;
  private String orderId;
  private OrdStatus ordStatus;
  private Instant transactTime;
  
  @SerializedName("@type")
  private final String type = "OrderCancelReject";

  /**
   * @param bufferSupplier
   * 
   */
  public JsonMutableOrderCancelReject(BufferSupplier bufferSupplier) {
    super(bufferSupplier);
  }


  @Override
  public void setClOrdId(String clOrdId) {
    this.clOrdId = clOrdId;
  }

  @Override
  public void setCxlRejReason(CxlRejReason cxlRejReason) {
    this.cxlRejReason = cxlRejReason;
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
  public void setTransactTime(Instant transactTime) {
    this.transactTime = transactTime;
  }

}
