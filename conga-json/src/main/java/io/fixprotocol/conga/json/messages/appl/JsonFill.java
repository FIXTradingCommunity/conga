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

import java.math.BigDecimal;

import io.fixprotocol.conga.messages.appl.ExecutionReport;

/**
 * @author Don Mendelson
 *
 */
public class JsonFill implements ExecutionReport.Fill  {

  private BigDecimal fillPx;
  private int fillQty;
  
  public BigDecimal getFillPx() {
    return fillPx;
  }
  public int getFillQty() {
    return fillQty;
  }


}
