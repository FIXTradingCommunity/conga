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

import io.fixprotocol.conga.json.util.CharBufferReader;
import io.fixprotocol.conga.messages.appl.Message;
import io.fixprotocol.conga.messages.appl.MessageException;
import io.fixprotocol.conga.messages.appl.RequestMessageFactory;

/**
 * @author Don Mendelson
 *
 */
public class JsonRequestMessageFactory implements RequestMessageFactory {

  private final static Gson gson = new Gson();
  private final JsonParser parser = new JsonParser();
  private ByteBuffer buffer;

  public Message wrap(ByteBuffer buffer) throws MessageException {
    this.buffer = buffer;
    CharBufferReader reader = new CharBufferReader(buffer.asCharBuffer());
    JsonObject object = parser.parse(reader).getAsJsonObject();
    String type = object.get("@type").getAsString();
    switch (type) {
      case "OrderCancelRequest":
        return getOrderCancelRequest();
      case "NewOrderSingle":
        return getNewOrderSingle();
      case "NotApplied":
        return getNotApplied();
      default:
        throw new MessageException("Unknown message type");
    }
  }

  public JsonNewOrderSingle getNewOrderSingle() {
    CharBufferReader reader = new CharBufferReader(buffer.asCharBuffer());
    return gson.fromJson(reader, JsonNewOrderSingle.class);
  }

  public JsonOrderCancelRequest getOrderCancelRequest() {
    CharBufferReader reader = new CharBufferReader(buffer.asCharBuffer());
    return gson.fromJson(reader, JsonOrderCancelRequest.class);
  }

  public JsonNotApplied getNotApplied() {
    CharBufferReader reader = new CharBufferReader(buffer.asCharBuffer());
    return gson.fromJson(reader, JsonNotApplied.class);
  }

}
