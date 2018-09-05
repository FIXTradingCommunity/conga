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

/**
 * @author Don Mendelson
 *
 */
public class JsonMutableEstablish extends JsonMutableMessage {


  private byte[] credentials;
  private long heartbeatInterval;
  private long nextSeqNo;
  private byte[] sessionId;
  private long timestamp;

  @SerializedName("@type")
  private String type = "Establish";

  /**
   * Constructor
   *
   * @param bufferSupplier supplies a buffer on demand
   */
  public JsonMutableEstablish(BufferSupplier bufferSupplier) {
    super(bufferSupplier);
  }

  public byte[] getCredentials() {
    return credentials;
  }

  public long getHeartbeatInterval() {
    return heartbeatInterval;
  }

  public long getNextSeqNo() {
    return nextSeqNo;
  }

  public byte[] getSessionId() {
    return sessionId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public JsonMutableEstablish set(byte[] sessionId, long timestamp, long heartbeatInterval,
      long nextSeqNo, byte[] credentials) {
    this.sessionId = sessionId;
    this.timestamp = timestamp;
    this.heartbeatInterval = heartbeatInterval;
    this.nextSeqNo = nextSeqNo;
    this.credentials = credentials;
    return this;
  }
}
