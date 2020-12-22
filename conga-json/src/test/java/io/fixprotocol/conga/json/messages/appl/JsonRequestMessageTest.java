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


import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.SingleBufferSupplier;
import io.fixprotocol.conga.io.MessageLogWriter;
import io.fixprotocol.conga.messages.appl.MutableNewOrderSingle;
import io.fixprotocol.conga.messages.appl.MutableOrderCancelRequest;
import io.fixprotocol.conga.messages.appl.OrdType;
import io.fixprotocol.conga.messages.appl.Side;

/**
 * @author Don Mendelson
 *
 */
public class JsonRequestMessageTest {
  
  private JsonMutableRequestMessageFactory mutableFactory;
  private JsonRequestMessageFactory factory;
  private BufferSupplier bufferSupplier;

  private MessageLogWriter writer;
  private static final Path path = FileSystems.getDefault().getPath("target/test", "json.log");

  @BeforeAll
  public static void setUpOnce() {
    new File("target/test").mkdirs();
    
    try {
      FileChannel channel = FileChannel.open(path, StandardOpenOption.APPEND);
      channel.truncate(0);
      channel.close();
    } catch (IOException e) {

    }
  }
  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  public void setUp() throws Exception {
    final ByteBuffer buffer = ByteBuffer.allocate(1024);
    buffer.order(ByteOrder.nativeOrder());
    bufferSupplier = new SingleBufferSupplier(buffer);
    mutableFactory = new JsonMutableRequestMessageFactory(bufferSupplier);
    factory = new JsonRequestMessageFactory();
    writer = new MessageLogWriter(path, true);
    writer.open();
  }
  
  @AfterEach
  public void tearDown() throws Exception {
    if (writer != null) {
      writer.close();
    }
  }

  @Test
  public void newOrderSingle() throws Exception {
    MutableNewOrderSingle mutableOrder = mutableFactory.getNewOrderSingle();
    String clOrdId = "C0001";
    mutableOrder.setClOrdId(clOrdId);
    int orderQty = 3;
    mutableOrder.setOrderQty(orderQty);
    OrdType ordType = OrdType.Limit;
    mutableOrder.setOrdType(ordType);
    BigDecimal price = new BigDecimal("54.32");
    mutableOrder.setPrice(price);
    Side side = Side.Sell;
    mutableOrder.setSide(side);
    String source = "S1";
    mutableOrder.setSource(source);
    String symbol = "XYZ";
    mutableOrder.setSymbol(symbol);
    Instant transactTime = Instant.now();
    mutableOrder.setTransactTime(transactTime);
    ByteBuffer buffer = mutableOrder.toBuffer();
    writer.writeAsync(buffer.duplicate(), (short) 0xF500).get();
    
    factory.wrap(buffer);
    JsonNewOrderSingle order = factory.getNewOrderSingle();
    assertEquals(clOrdId, order.getClOrdId());
    assertEquals(orderQty, order.getOrderQty());
    assertEquals(ordType, order.getOrdType());
    assertEquals(price, order.getPrice());
    assertEquals(side, order.getSide());
    assertEquals(symbol, order.getSymbol());
    assertEquals(transactTime, order.getTransactTime());
  }

  @Test
  public void orderCancelRequest() throws Exception {
    MutableOrderCancelRequest mutableCancel = mutableFactory.getOrderCancelRequest();
    String clOrdId = "C0001";
    mutableCancel.setClOrdId(clOrdId);
    Side side = Side.Buy;
    mutableCancel.setSide(side);
    String symbol = "XYZ";
    mutableCancel.setSymbol(symbol);
    Instant transactTime = Instant.now();
    mutableCancel.setTransactTime(transactTime);
    ByteBuffer buffer = mutableCancel.toBuffer();
    writer.writeAsync(buffer.duplicate(), (short) 0xF500).get();
    
    factory.wrap(buffer);
    JsonOrderCancelRequest cancel = factory.getOrderCancelRequest();
    assertEquals(clOrdId, cancel.getClOrdId());
    assertEquals(side, cancel.getSide());
    assertEquals(symbol, cancel.getSymbol());
    assertEquals(transactTime, cancel.getTransactTime());
  }

}
