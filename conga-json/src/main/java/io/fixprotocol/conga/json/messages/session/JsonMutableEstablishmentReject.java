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
import io.fixprotocol.conga.session.EstablishmentReject;

/**
 * @author Don Mendelson
 *
 */
public class JsonMutableEstablishmentReject extends JsonMutableMessage {

  private byte[] reason;
  private EstablishmentReject rejectCode;
  private byte[] sessionId;
  private long timestamp;
  
  @SerializedName("@type")
  private final String type = "EstablishmentReject";
  
  /**
   * Constructor
   * @param bufferSupplier supplies a buffer on demand
   */
  public JsonMutableEstablishmentReject(BufferSupplier bufferSupplier) {
    super(bufferSupplier);
  }

  public byte[] getReason() {
    return reason;
  }

  public EstablishmentReject getRejectCode() {
    return rejectCode;
  }

  public byte[] getSessionId() {
    return sessionId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public JsonMutableEstablishmentReject set(byte[] sessionId, long timestamp,
      EstablishmentReject rejectCode, byte[] reason) {
    this.sessionId = sessionId;
    this.timestamp = timestamp;
    this.rejectCode = rejectCode;
    this.reason = reason;
    return this;
  }
}
