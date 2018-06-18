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

package io.fixprotocol.conga.session.sbe;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.agrona.concurrent.UnsafeBuffer;

import io.fixprotocol.conga.sbe.messages.fixp.MessageHeaderEncoder;
import io.fixprotocol.conga.sbe.messages.fixp.SequenceEncoder;
import io.fixprotocol.conga.session.Session;

/**
 * FIXP session with SBE encoding
 * <p>
 * Limitations: does not support multiplexing.
 * @author Don Mendelson
 *
 */
public abstract class SbeSession extends Session {

  private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
  private final SequenceEncoder sequenceEncoder = new SequenceEncoder();
  private final ByteBuffer sequenceBuffer = ByteBuffer.allocateDirect(16);
  private final UnsafeBuffer sequenceMutableBuffer = new UnsafeBuffer();

  public SbeSession() {
    sequenceMutableBuffer.wrap(sequenceBuffer);
    sequenceEncoder.wrapAndApplyHeader(sequenceMutableBuffer, 0, headerEncoder);
  }
  
  @Override
  protected long doSendHeartbeat(long nextSeqNo) throws IOException, InterruptedException {
    sequenceEncoder.nextSeqNo(nextSeqNo);
    doSendMessage(sequenceBuffer);
    return nextSeqNo;
  }
}
