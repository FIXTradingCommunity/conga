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
import java.util.Timer;

import org.agrona.concurrent.UnsafeBuffer;

import io.fixprotocol.conga.sbe.messages.fixp.MessageHeaderDecoder;
import io.fixprotocol.conga.sbe.messages.fixp.MessageHeaderEncoder;
import io.fixprotocol.conga.sbe.messages.fixp.NotAppliedEncoder;
import io.fixprotocol.conga.sbe.messages.fixp.SequenceDecoder;
import io.fixprotocol.conga.sbe.messages.fixp.SequenceEncoder;
import io.fixprotocol.conga.session.Session;

/**
 * FIXP session with SBE encoding
 * <p>
 * Limitations: does not support multiplexing.
 * 
 * @author Don Mendelson
 *
 */
public abstract class SbeSession extends Session {

  private final UnsafeBuffer directBuffer = new UnsafeBuffer();
  private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
  private final ByteBuffer notAppliedBuffer = ByteBuffer.allocateDirect(32);
  private final NotAppliedEncoder notAppliedEncoder = new NotAppliedEncoder();
  private final UnsafeBuffer notAppliedMutableBuffer = new UnsafeBuffer();
  private final ByteBuffer sequenceBuffer = ByteBuffer.allocateDirect(16);
  private final SequenceDecoder sequenceDecoder = new SequenceDecoder();
  private final SequenceEncoder sequenceEncoder = new SequenceEncoder();
  private final UnsafeBuffer sequenceMutableBuffer = new UnsafeBuffer();


  protected SbeSession(Timer timer, long heartbeatInterval) {
    super(timer, heartbeatInterval);
    final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    sequenceMutableBuffer.wrap(sequenceBuffer);
    sequenceEncoder.wrapAndApplyHeader(sequenceMutableBuffer, 0, headerEncoder);
    sequenceBuffer.limit(headerEncoder.encodedLength() + sequenceEncoder.encodedLength());
    notAppliedMutableBuffer.wrap(notAppliedBuffer);
    notAppliedEncoder.wrapAndApplyHeader(notAppliedMutableBuffer, 0, headerEncoder);
    notAppliedBuffer.limit(headerEncoder.encodedLength() + notAppliedEncoder.encodedLength());
  }

  @Override
  protected void doSendHeartbeat(long nextSeqNo) throws IOException, InterruptedException {
    sequenceEncoder.nextSeqNo(nextSeqNo);
    doSendMessage(sequenceBuffer.duplicate());
  }

  protected void doSendNotApplied(long fromSeqNo, long count)
      throws IOException, InterruptedException {
    notAppliedEncoder.fromSeqNo(fromSeqNo);
    notAppliedEncoder.count(count);
    doSendMessage(notAppliedBuffer.duplicate());
  }

  @Override
  protected MessageType getMessageType(ByteBuffer buffer) {
    MessageType messageType = MessageType.IGNORE;
    directBuffer.wrap(buffer);
    headerDecoder.wrap(directBuffer, 0);
    if (SequenceEncoder.SCHEMA_ID != headerDecoder.schemaId()) {
      messageType = MessageType.APPLICATION;
    } else {
      int templateId = headerDecoder.templateId();
      switch (templateId) {
        case SequenceEncoder.TEMPLATE_ID:
          messageType = MessageType.SEQUENCE;
          break;
        case NotAppliedEncoder.TEMPLATE_ID:
          messageType = MessageType.NOT_APPLIED;
          break;
      }
    }

    return messageType;
  }

  @Override
  protected long getNextSequenceNumber(ByteBuffer buffer) {
    directBuffer.wrap(buffer);
    headerDecoder.wrap(directBuffer, 0);
    sequenceDecoder.wrap(directBuffer, headerDecoder.encodedLength(), headerDecoder.blockLength(),
        headerDecoder.version());
    return sequenceDecoder.nextSeqNo();
  }
}
