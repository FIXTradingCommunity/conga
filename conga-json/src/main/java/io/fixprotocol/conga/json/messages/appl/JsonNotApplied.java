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

import com.google.gson.annotations.SerializedName;

import io.fixprotocol.conga.messages.appl.Message;
import io.fixprotocol.conga.messages.appl.NotApplied;

/**
 * @author Don Mendelson
 *
 */
public class JsonNotApplied implements NotApplied, Message {

  private long count;
  private long fromSeqNo;
  private transient String source;

  @SerializedName("@type")
  private String type = "NotApplied";

  @Override
  public long getCount() {
    return count;
  }

  @Override
  public long getFromSeqNo() {
    return fromSeqNo;
  }

  /**
   * @return the source
   */
  public String getSource() {
    return source;
  }

  /**
   * @param source the source to set
   */
  public void setSource(String source) {
    this.source = source;
  }

}
