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

import java.math.BigDecimal;

import io.fixprotocol.conga.messages.appl.OrdType;
import io.fixprotocol.conga.messages.appl.Side;

/**
 * @author Don Mendelson
 *
 */
public class BookSweepGenerator extends RequestGenerator implements Runnable {


  private int orderNum = 0;


  /**
   * @param encoding
   * @param fileName
   */
  public BookSweepGenerator(String encoding, String fileName) {
    super(encoding, fileName);
  }

  /**
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    for (String encoding : new String[] {/*"SBE", */"JSON"}) {
      String fileName = String.format("BookSweep-%s.dat", encoding);
      try (BookSweepGenerator generator = new BookSweepGenerator(encoding, fileName)) {
        generator.init();
        generator.run();
      }
    }

  }

  public void run() {
    String symbol = "AMZN";
    OrdType ordType = OrdType.Limit;
    BigDecimal tick = new BigDecimal("0.05");
    BigDecimal price = new BigDecimal("1.00");
    int totalQty = 0;
    
    for (int i=1; i <= 10; i++) {
      price = price.add(tick);
      String clOrdId = genClOrdId();
      int orderQty = i;
      totalQty += orderQty;
      Side side = Side.Buy;
      generateOrder(clOrdId, orderQty, ordType, price, side, symbol);
    }
    generateOrder(genClOrdId(), totalQty, ordType, new BigDecimal("1.00"), Side.Sell, symbol);
  }


  private String genClOrdId() {
    return String.format("CL%06d", ++orderNum );
  }

}
