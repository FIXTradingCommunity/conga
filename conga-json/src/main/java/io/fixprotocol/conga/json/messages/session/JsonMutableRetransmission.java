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
import io.fixprotocol.conga.session.SequenceRange;

/**
 * @author Don Mendelson
 *
 */
public class JsonMutableRetransmission extends JsonMutableMessage {

  private long count;
  private long fromSeqNo;
  private long requestTimestamp;
  private byte[] sessionId;

  @SerializedName("@type")
  private final String type = "Retransmission";

  /**
   * Constructor
   * @param bufferSupplier supplies a buffer on demand
   */
  public JsonMutableRetransmission(BufferSupplier bufferSupplier) {
    super(bufferSupplier);
  }

  public long getCount() {
    return count;
  }

  public long getFromSeqNo() {
    return fromSeqNo;
  }

  public long getRequestTimestamp() {
    return requestTimestamp;
  }

  public byte[] getSessionId() {
    return sessionId;
  }

  /**
   * Sets message values for a range of messages to retransmit
   * @param sessionId FIXP session identifier
   * @param range a range of message sequence numbers
   */
  public void set(byte[] sessionId, SequenceRange range) {
    this.sessionId = sessionId;
    this.requestTimestamp = range.getTimestamp();
    this.fromSeqNo = range.getFromSeqNo();
    this.count = range.getCount();
  }

}
