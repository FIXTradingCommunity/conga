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

package io.fixprotocol.conga.json.messages;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.json.messages.appl.JsonMutableRequestMessageFactory;
import io.fixprotocol.conga.json.messages.appl.JsonMutableResponseMessageFactory;
import io.fixprotocol.conga.json.messages.appl.JsonRequestMessageFactory;
import io.fixprotocol.conga.json.messages.appl.JsonResponseMessageFactory;
import io.fixprotocol.conga.json.messages.session.JsonSessionMessenger;
import io.fixprotocol.conga.messages.appl.MutableRequestMessageFactory;
import io.fixprotocol.conga.messages.appl.MutableResponseMessageFactory;
import io.fixprotocol.conga.messages.appl.RequestMessageFactory;
import io.fixprotocol.conga.messages.appl.ResponseMessageFactory;
import io.fixprotocol.conga.messages.session.SessionMessenger;
import io.fixprotocol.conga.messages.spi.MessageProvider;


/**
 * @author Don Mendelson
 *
 */
public class JsonMessageProvider implements MessageProvider {

  @Override
  public MutableRequestMessageFactory getMutableRequestMessageFactory(
      BufferSupplier bufferSupplier) {
    return new JsonMutableRequestMessageFactory(bufferSupplier);
  }

  @Override
  public MutableResponseMessageFactory getMutableResponseMessageFactory(
      BufferSupplier bufferSupplier) {
    return new JsonMutableResponseMessageFactory(bufferSupplier);
  }

  @Override
  public RequestMessageFactory getRequestMessageFactory() {
    return new JsonRequestMessageFactory();
  }

  @Override
  public ResponseMessageFactory getResponseMessageFactory() {
    return new JsonResponseMessageFactory();
  }

  @Override
  public SessionMessenger getSessionMessenger() {
    return new JsonSessionMessenger();
  }

  @Override
  public String name() {
    return "JSON";
  }

  @Override
  public short encodingType() {
    return (short) 0xF500;
  }

}
