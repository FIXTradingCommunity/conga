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

package io.fixprotocol.conga.session;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Don Mendelson
 *
 */
public interface SessionMessenger {
  
  void decodeEstablishmentAckSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes);
  
  void decodeEstablishSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes);

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

  ByteBuffer encodeEstablish(byte[] sessionId, long timestamp, long heartbeatInterval,
      long nextSeqNo, byte[] credentials) throws IOException, InterruptedException;

  ByteBuffer encodeEstablishAck(byte[] sessionId, long timestamp,
      long heartbeatInterval, long nextSeqNo) throws IOException, InterruptedException;

  ByteBuffer encodeEstablishReject(byte[] sessionId, long timestamp,
      EstablishmentReject rejectCode, byte[] reason) throws IOException, InterruptedException;

  ByteBuffer encodeNegotiate(byte[] sessionId, long timestamp, FlowType clientFlow,
      byte[] credentials) throws IOException, InterruptedException;

  ByteBuffer encodeNegotiationReject(byte[] sessionId, long requestTimestamp,
      NegotiationReject rejectCode, byte[] reason) throws IOException, InterruptedException;

  ByteBuffer encodeNegotiationResponse(byte[] sessionId, long requestTimestamp,
      FlowType serverFlow, byte[] credentials) throws IOException, InterruptedException;

  ByteBuffer encodeNotApplied(long fromSeqNo, long count)
      throws IOException, InterruptedException;

  ByteBuffer encodeRetransmission(byte[] sessionId, SequenceRange range)
      throws IOException, InterruptedException;

  ByteBuffer encodeRetransmitRequest(byte[] sessionId, SequenceRange range)
      throws IOException, InterruptedException;

  ByteBuffer encodeSequence(long nextSeqNo) throws IOException, InterruptedException;

  SessionMessageType getMessageType(ByteBuffer buffer);

  void getRetransmissionSequenceRange(ByteBuffer buffer, SequenceRange range);

  void getRetransmitRequestSequenceRange(ByteBuffer buffer, SequenceRange range);

  /**
   * Initialize
   * 
   * @param isClientSession {@code true} if a client session, otherwise a server session
   */
  void init(boolean isClientSession);

}
