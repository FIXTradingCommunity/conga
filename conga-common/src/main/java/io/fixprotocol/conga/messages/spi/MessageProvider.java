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

package io.fixprotocol.conga.messages.spi;

import java.util.ServiceLoader;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.messages.appl.MutableRequestMessageFactory;
import io.fixprotocol.conga.messages.appl.MutableResponseMessageFactory;
import io.fixprotocol.conga.messages.appl.RequestMessageFactory;
import io.fixprotocol.conga.messages.appl.ResponseMessageFactory;
import io.fixprotocol.conga.messages.session.SessionMessenger;

/**
 * @author Don Mendelson
 *
 */
public interface MessageProvider {
  
  /**
   * Locate a service provider for an application message encoding
   * @param name encoding name
   * @return a service provider
   */
  static MessageProvider provider(String name) {
    ServiceLoader<MessageProvider> loader = ServiceLoader.load(MessageProvider.class);
    for (MessageProvider provider : loader) {
      if (provider.name().equals(name)) {
        return provider;
      }
    }
    throw new RuntimeException("No MessageProvider found");
  }
  
  /**
   * 
   * @return encoding name
   */
  String name();
  
  RequestMessageFactory getRequestMessageFactory();
  
  ResponseMessageFactory getResponseMessageFactory();
  
  MutableRequestMessageFactory getMutableRequestMessageFactory(BufferSupplier bufferSupplier);
  
  MutableResponseMessageFactory getMutableResponseMessageFactory(BufferSupplier bufferSupplier);
  
  SessionMessenger getSessionMessenger();
}
