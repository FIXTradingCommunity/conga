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

package io.fixprotocol.conga.sbe.messages.appl;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.messages.appl.MutableExecutionReport;
import io.fixprotocol.conga.messages.appl.MutableOrderCancelReject;
import io.fixprotocol.conga.messages.appl.MutableResponseMessageFactory;

/**
 * SBE message factory for exchange responses
 * 
 * <p>
 * Thread-safe, but only one message of each type my be encoded at a time per thread.
 * 
 * @author Don Mendelson
 *
 */
public class SbeMutableResponseMessageFactory implements MutableResponseMessageFactory {

  private final BufferSupplier bufferSupplier;

  private final ThreadLocal<SbeMutableOrderCancelReject> cancelRejectThreadLocal =
      ThreadLocal.withInitial(SbeMutableOrderCancelReject::new);

  private final ThreadLocal<SbeMutableExecutionReport> executionReportThreadLocal =
      ThreadLocal.withInitial(SbeMutableExecutionReport::new);

  /**
   * Constructor
   * 
   * @param bufferSupplier supplies ByteBuffer to populate for each message
   */
  public SbeMutableResponseMessageFactory(BufferSupplier bufferSupplier) {
    this.bufferSupplier = bufferSupplier;
  }

  @Override
  public MutableExecutionReport getExecutionReport() {
    var executionReport = executionReportThreadLocal.get();
    executionReport.wrap(bufferSupplier);
    return executionReport;
  }

  @Override
  public MutableOrderCancelReject getOrderCancelReject() {
    final var cancelReject = cancelRejectThreadLocal.get();
    cancelReject.wrap(bufferSupplier);
    return cancelReject;
  }

}
