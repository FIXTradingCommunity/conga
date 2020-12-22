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
public class JsonMutableNotApplied extends JsonMutableMessage {

  private long fromSeqNo;
  private long count;
  
  @SerializedName("@type")
  private final String type = "NotApplied";

  /**
   * Constructor
   * @param bufferSupplier supplies a buffer on demand
   */
  public JsonMutableNotApplied(BufferSupplier bufferSupplier) {
    super(bufferSupplier);
  }

  /**
   * Sets message values for a range of sequence numbers
   * @param fromSeqNo the first sequence number of messages not applied
   * @param count the number of messages not applied
   */
  public void set(long fromSeqNo, long count) {
    this.fromSeqNo = fromSeqNo;
    this.count = count;
  }

}
