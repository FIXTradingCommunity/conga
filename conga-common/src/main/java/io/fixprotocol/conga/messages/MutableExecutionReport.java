package io.fixprotocol.conga.messages;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;

public interface MutableExecutionReport extends MutableMessage {

  interface MutableFill {

    void setFillPx(BigDecimal fillPx);

    void setFillQty(int fillQty);
  }

  /**
   * Allocate space for fills
   * @param count number of fills
   */
  void setFillCount(int count);
  
  /**
   * Access the next fill
   * @return a fill to populate
   * @throws NoSuchElementException if invocations exceeds the count specified by {@link #setFillCount(int)}
   */ 
  MutableFill nextFill();

  void setClOrdId(String clOrdId);

  void setCumQty(int cumQty);

  void setExecId(String execId);

  void setExecType(ExecType execType);

  void setLeavesQty(int leavesQty);

  void setOrderId(String orderId);

  void setOrdStatus(OrdStatus ordStatus);

  void setSide(Side side);

  void setSymbol(String symbol);
  
  void setTransactTime(Instant time);

}
