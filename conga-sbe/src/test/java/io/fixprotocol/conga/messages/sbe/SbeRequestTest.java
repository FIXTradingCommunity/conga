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

package io.fixprotocol.conga.messages.sbe;



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;

import io.fixprotocol.conga.buffer.SingleBufferSupplier;
import io.fixprotocol.conga.messages.appl.Message;
import io.fixprotocol.conga.messages.appl.MessageException;
import io.fixprotocol.conga.messages.appl.MutableNewOrderSingle;
import io.fixprotocol.conga.messages.appl.MutableOrderCancelRequest;
import io.fixprotocol.conga.messages.appl.NewOrderSingle;
import io.fixprotocol.conga.messages.appl.OrdType;
import io.fixprotocol.conga.messages.appl.OrderCancelRequest;
import io.fixprotocol.conga.messages.appl.Side;
import io.fixprotocol.conga.sbe.messages.appl.SbeMutableRequestMessageFactory;
import io.fixprotocol.conga.sbe.messages.appl.SbeRequestMessageFactory;

/**
 * @author Don Mendelson
 *
 */
public class SbeRequestTest {
  
  private SbeMutableRequestMessageFactory mutableMessageFactory;
  private SbeRequestMessageFactory messageFactory;
  private ByteBuffer buffer;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    buffer = ByteBuffer.allocate(1024);
    SingleBufferSupplier bufferSupplier = new SingleBufferSupplier(buffer);
    mutableMessageFactory = new SbeMutableRequestMessageFactory(bufferSupplier);
    messageFactory = new SbeRequestMessageFactory();
  }

  @Test
  public void order() throws MessageException {
    MutableNewOrderSingle encoder = mutableMessageFactory.getNewOrderSingle();
    String clOrdId = "CL0001";
    encoder.setClOrdId(clOrdId);
    int orderQty = 7;
    encoder.setOrderQty(orderQty);
    OrdType ordType = OrdType.Limit;
    encoder.setOrdType(ordType);
    BigDecimal price = BigDecimal.valueOf(5432, 2);
    encoder.setPrice(price);
    Side side = Side.Sell;
    encoder.setSide(side);
    String symbol = "SYM1";
    encoder.setSymbol(symbol);
    Instant transactTime = Instant.ofEpochMilli(10000L);
    encoder.setTransactTime(transactTime);
    buffer.flip();
    
    Message message = messageFactory.wrap(buffer);
    if (message instanceof NewOrderSingle) {
      NewOrderSingle decoder = (NewOrderSingle) message;
      assertEquals(clOrdId, decoder.getClOrdId());
      assertEquals(orderQty, decoder.getOrderQty());
      assertEquals(ordType, decoder.getOrdType());
      assertEquals(price, decoder.getPrice());
      assertEquals(side, decoder.getSide());
      assertEquals(symbol, decoder.getSymbol());
      assertEquals(transactTime, decoder.getTransactTime());
    } else {
      fail("Wrong message type");
    }
    
    encoder.release();
  }
  
  @Test
  public void cancel() throws MessageException {
    MutableOrderCancelRequest encoder = mutableMessageFactory.getOrderCancelRequest();
    String clOrdId = "CL0001";
    encoder.setClOrdId(clOrdId);
    Side side = Side.Sell;
    encoder.setSide(side);
    String symbol = "SYM1";
    encoder.setSymbol(symbol);
    Instant transactTime = Instant.ofEpochMilli(10000L);
    encoder.setTransactTime(transactTime); 
    buffer.flip();
    
    Message message = messageFactory.wrap(buffer);
    if (message instanceof OrderCancelRequest) {
      OrderCancelRequest decoder = (OrderCancelRequest) message;
      assertEquals(clOrdId, decoder.getClOrdId());
      assertEquals(side, decoder.getSide());
      assertEquals(symbol, decoder.getSymbol());
      assertEquals(transactTime, decoder.getTransactTime());
    } else {
      fail("Wrong message type");
    }
    
    encoder.release();
  }
}
