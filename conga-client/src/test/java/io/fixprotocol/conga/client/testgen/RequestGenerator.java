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

package io.fixprotocol.conga.client.testgen;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.SingleBufferSupplier;
import io.fixprotocol.conga.io.MessageLogWriter;
import io.fixprotocol.conga.messages.appl.MutableNewOrderSingle;
import io.fixprotocol.conga.messages.appl.MutableOrderCancelRequest;
import io.fixprotocol.conga.messages.appl.MutableRequestMessageFactory;
import io.fixprotocol.conga.messages.appl.OrdType;
import io.fixprotocol.conga.messages.appl.Side;
import io.fixprotocol.conga.messages.spi.MessageProvider;

/**
 * @author Don Mendelson
 *
 */
public class RequestGenerator implements AutoCloseable {

  private final String encoding;
  private final Consumer<Throwable> errorListener = Throwable::printStackTrace;
  private final MessageLogWriter writer;
  private MutableRequestMessageFactory requestFactory;
  private short encodingCode;


  public RequestGenerator(String encoding, String fileName) {
    this.encoding = encoding;
    Path path = FileSystems.getDefault().getPath(fileName);
    writer = new MessageLogWriter(path, true, errorListener);
  }

  @Override
  public void close() throws Exception {
    writer.close();
  }

  public void init() throws IOException {
    MessageProvider messageProvider = provider(encoding);
    encodingCode = messageProvider.encodingType();
    final ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.nativeOrder());
    BufferSupplier requestBufferSupplier = new SingleBufferSupplier(buffer);
    requestFactory = messageProvider.getMutableRequestMessageFactory(requestBufferSupplier);
    writer.open();
  }
  
  public void generateOrder(String clOrdId, int orderQty, OrdType ordType, BigDecimal price, Side side, String symbol) {
    MutableNewOrderSingle order = requestFactory.getNewOrderSingle();
    order.setClOrdId(clOrdId);
    order.setOrderQty(orderQty);
    order.setOrdType(ordType);
    order.setPrice(price);
    order.setSide(side);
    order.setSymbol(symbol);
    order.setTransactTime(Instant.now());
    writer.write(order.toBuffer(), encodingCode);
    order.release();
  }
  
  public void generateCancel(String clOrdId, Side side, String symbol) {
    MutableOrderCancelRequest cancel = requestFactory.getOrderCancelRequest();
    cancel.setClOrdId(clOrdId);
    cancel.setSide(side);
    cancel.setSymbol(symbol);
    cancel.setTransactTime(Instant.now());
    writer.write(cancel.toBuffer(), encodingCode);
    cancel.release();
  }

  private MessageProvider provider(String name) {
    ServiceLoader<MessageProvider> loader = ServiceLoader.load(MessageProvider.class);
    for (MessageProvider provider : loader) {
      if (provider.name().equals(name)) {
        return provider;
      }
    }
    throw new RuntimeException("No MessageProvider found");
  }

}
