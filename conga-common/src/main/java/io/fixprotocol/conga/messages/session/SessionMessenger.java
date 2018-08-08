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

package io.fixprotocol.conga.messages.session;

import java.nio.ByteBuffer;

import io.fixprotocol.conga.messages.appl.MutableMessage;
import io.fixprotocol.conga.session.EstablishmentReject;
import io.fixprotocol.conga.session.FlowType;
import io.fixprotocol.conga.session.NegotiationReject;
import io.fixprotocol.conga.session.SequenceRange;
import io.fixprotocol.conga.session.SessionAttributes;
import io.fixprotocol.conga.session.SessionMessageType;
import io.fixprotocol.conga.session.SessionSequenceAttributes;

/**
 * Abstraction of session message encoders and decoders
 * <p>
 * Encode methods should provide a ByteBuffer that is ready to transmit. The position should
 * be 0 and limit set to the message size.
 * 
 * @author Don Mendelson
 *
 */
public interface SessionMessenger {
  
  void decodeEstablishmentAckSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes);
  
  void decodeEstablishSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes);

  void decodeFinishedReceiving(ByteBuffer buffer,
      SessionSequenceAttributes sessionSequenceAttributes);

  void decodeFinishedSending(ByteBuffer buffer,
      SessionSequenceAttributes sessionSequenceAttributes);
  
  void decodeNegotiateSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes);
  
  void decodeNegotiationResponseSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes);

  /**
   * Decode a FIXP Sequence message
   * @param buffer holds an encoded Sequence message
   * @return next message sequence number
   */
  long decodeSequence(ByteBuffer buffer);
  
  MutableMessage encodeEstablish(byte[] sessionId, long timestamp, long heartbeatInterval,
      long nextSeqNo, byte[] credentials);
  
  MutableMessage encodeEstablishmentAck(byte[] sessionId, long timestamp,
      long heartbeatInterval, long nextSeqNo);

  MutableMessage encodeEstablishmentReject(byte[] sessionId, long timestamp,
      EstablishmentReject rejectCode, byte[] reason);

  MutableMessage encodeFinishedReceiving(byte[] sessionId);

  MutableMessage encodeFinishedSending(byte[] sessionId, long lastSeqNo);

  MutableMessage encodeNegotiate(byte[] sessionId, long timestamp, FlowType clientFlow,
      byte[] credentials);

  MutableMessage encodeNegotiationReject(byte[] sessionId, long requestTimestamp,
      NegotiationReject rejectCode, byte[] reason);

  MutableMessage encodeNegotiationResponse(byte[] sessionId, long requestTimestamp,
      FlowType serverFlow, byte[] credentials);

  MutableMessage encodeNotApplied(long fromSeqNo, long count);

  MutableMessage encodeRetransmission(byte[] sessionId, SequenceRange range);

  MutableMessage encodeRetransmitRequest(byte[] sessionId, SequenceRange range);

  MutableMessage encodeSequence(long nextSeqNo);

  SessionMessageType getMessageType(ByteBuffer buffer);

  void decodeRetransmissionSequenceRange(ByteBuffer buffer, SequenceRange range);

  void decodeRetransmitRequestSequenceRange(ByteBuffer buffer, SequenceRange range);

  /**
   * Initialize
   * 
   * @param isClientSession {@code true} if a client session, otherwise a server session
   */
  void init(boolean isClientSession);

}
