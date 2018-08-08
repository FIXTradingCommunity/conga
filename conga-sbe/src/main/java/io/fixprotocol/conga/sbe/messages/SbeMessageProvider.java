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

package io.fixprotocol.conga.sbe.messages;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.messages.appl.MutableRequestMessageFactory;
import io.fixprotocol.conga.messages.appl.MutableResponseMessageFactory;
import io.fixprotocol.conga.messages.appl.RequestMessageFactory;
import io.fixprotocol.conga.messages.appl.ResponseMessageFactory;
import io.fixprotocol.conga.messages.session.SessionMessenger;
import io.fixprotocol.conga.messages.spi.MessageProvider;
import io.fixprotocol.conga.sbe.messages.appl.SbeMutableRequestMessageFactory;
import io.fixprotocol.conga.sbe.messages.appl.SbeMutableResponseMessageFactory;
import io.fixprotocol.conga.sbe.messages.appl.SbeRequestMessageFactory;
import io.fixprotocol.conga.sbe.messages.appl.SbeResponseMessageFactory;
import io.fixprotocol.conga.sbe.messages.session.SbeSessionMessenger;

/**
 * Implementation of MessageProvider for Simple Binary Encoding
 * 
 * @author Don Mendelson
 *
 */
public class SbeMessageProvider implements MessageProvider {

  @Override
  public MutableRequestMessageFactory getMutableRequestMessageFactory(
      BufferSupplier bufferSupplier) {
    return new SbeMutableRequestMessageFactory(bufferSupplier);
  }

  @Override
  public MutableResponseMessageFactory getMutableResponseMessageFactory(
      BufferSupplier bufferSupplier) {
    return new SbeMutableResponseMessageFactory(bufferSupplier);
  }

  @Override
  public RequestMessageFactory getRequestMessageFactory() {
    return new SbeRequestMessageFactory();
  }

  @Override
  public ResponseMessageFactory getResponseMessageFactory() {
    return new SbeResponseMessageFactory();
  }

  @Override
  public SessionMessenger getSessionMessenger() {
    return new SbeSessionMessenger();
  }

  @Override
  public String name() {
    return "SBE";
  }

}
