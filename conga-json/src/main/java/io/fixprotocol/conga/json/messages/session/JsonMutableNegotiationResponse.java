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

package io.fixprotocol.conga.json.messages.session;

import com.google.gson.annotations.SerializedName;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.json.messages.JsonMutableMessage;
import io.fixprotocol.conga.session.FlowType;

/**
 * @author Don Mendelson
 *
 */
public class JsonMutableNegotiationResponse extends JsonMutableMessage {

  private byte[] credentials;
  private long requestTimestamp;
  private FlowType serverFlow;
  private byte[] sessionId;

  @SerializedName("@type")
  private final String type = "NegotiationResponse";

  /**
   * Constructor
   * @param bufferSupplier supplies a buffer on demand
   */
  public JsonMutableNegotiationResponse(BufferSupplier bufferSupplier) {
    super(bufferSupplier);
  }

  public byte[] getCredentials() {
    return credentials;
  }

  public long getRequestTimestamp() {
    return requestTimestamp;
  }

  public FlowType getServerFlow() {
    return serverFlow;
  }

  public byte[] getSessionId() {
    return sessionId;
  }

  public JsonMutableNegotiationResponse set(byte[] sessionId, long requestTimestamp,
      FlowType serverFlow, byte[] credentials) {
    this.sessionId = sessionId;
    this.requestTimestamp = requestTimestamp;
    this.serverFlow = serverFlow;
    this.credentials = credentials;
    return this;
  }
}
