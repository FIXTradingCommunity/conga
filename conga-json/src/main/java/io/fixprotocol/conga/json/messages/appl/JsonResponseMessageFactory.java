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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fixprotocol.conga.json.messages.gson.JsonTranslatorFactory;
import io.fixprotocol.conga.messages.appl.Message;
import io.fixprotocol.conga.messages.appl.MessageException;
import io.fixprotocol.conga.messages.appl.ResponseMessageFactory;

/**
 * @author Don Mendelson
 *
 */
public class JsonResponseMessageFactory implements ResponseMessageFactory {

  private final static Gson gson = JsonTranslatorFactory.createTranslator();
  private final JsonParser parser = new JsonParser();
  private ByteBuffer buffer;

  public Message wrap(ByteBuffer buffer) throws MessageException {
    this.buffer = buffer;
    String string = bufferToString(this.buffer);
    JsonObject object = parser.parse(string).getAsJsonObject();
    String type = object.get("@type").getAsString();
    switch (type) {
      case "OrderCancelReject":
        return getOrderCancelReject();
      case "ExecutionReport":
        return getExecutionReport();
      case "NotApplied":
        return getNotApplied();
      default:
        throw new MessageException("Unknown message type");
    }
  }

  public JsonOrderCancelReject getOrderCancelReject() {
    String string = bufferToString(this.buffer);
    return gson.fromJson(string, JsonOrderCancelReject.class);
  }

  public JsonExecutionReport getExecutionReport() {
    String string = bufferToString(this.buffer);
    return gson.fromJson(string, JsonExecutionReport.class);
  }

  public JsonNotApplied getNotApplied() {
    String string = bufferToString(this.buffer);
    return gson.fromJson(string, JsonNotApplied.class);
  }
  
  private String bufferToString(ByteBuffer buffer) {
    ByteBuffer buf = buffer.duplicate();
    byte[] dst = new byte[buf.remaining()];
    buf.get(dst , 0, dst.length);
    return new String(dst);
  }

}
