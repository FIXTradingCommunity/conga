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

package io.fixprotocol.conga.json.messages.session;

import java.nio.ByteBuffer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.ThreadLocalBufferSupplier;
import io.fixprotocol.conga.json.messages.gson.JsonTranslatorFactory;
import io.fixprotocol.conga.json.util.CharBufferReader;
import io.fixprotocol.conga.messages.session.SessionMessenger;
import io.fixprotocol.conga.session.EstablishmentReject;
import io.fixprotocol.conga.session.FlowType;
import io.fixprotocol.conga.session.NegotiationReject;
import io.fixprotocol.conga.session.SequenceRange;
import io.fixprotocol.conga.session.SessionAttributes;
import io.fixprotocol.conga.session.SessionMessageType;
import io.fixprotocol.conga.session.SessionSequenceAttributes;

/**
 * @author Don Mendelson
 *
 */
public class JsonSessionMessenger implements SessionMessenger {
  private final static Gson gson = JsonTranslatorFactory.createTranslator();
  private final BufferSupplier bufferSupplier;
  private final JsonParser parser = new JsonParser();
  
  public JsonSessionMessenger() {
     this(new ThreadLocalBufferSupplier());
  }

  public JsonSessionMessenger(BufferSupplier bufferSupplier) {
    this.bufferSupplier = bufferSupplier;
  }

  public void decodeEstablishmentAckSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes) {
    CharBufferReader reader = new CharBufferReader(buffer.asCharBuffer());
    JsonMutableEstablishmentAck ack = gson.fromJson(reader, JsonMutableEstablishmentAck.class);
    sessionAttributes.sessionId(ack.getSessionId()).keepAliveInterval(ack.getHeartbeatInterval())
    .nextSeqNo(ack.getNextSeqNo()).timestamp(ack.getTimestamp());
  }

  public void decodeEstablishSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes) {
    CharBufferReader reader = new CharBufferReader(buffer.asCharBuffer());
    JsonMutableEstablish establish = gson.fromJson(reader, JsonMutableEstablish.class);
    sessionAttributes.sessionId(establish.getSessionId()).keepAliveInterval(establish.getHeartbeatInterval())
    .nextSeqNo(establish.getNextSeqNo()).timestamp(establish.getTimestamp()).credentials(establish.getCredentials());
  }

  public void decodeFinishedReceiving(ByteBuffer buffer,
      SessionSequenceAttributes sessionSequenceAttributes) {
    // TODO Auto-generated method stub

  }

  public void decodeFinishedSending(ByteBuffer buffer,
      SessionSequenceAttributes sessionSequenceAttributes) {
    // TODO Auto-generated method stub

  }

  public void decodeNegotiateSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes) {
    CharBufferReader reader = new CharBufferReader(buffer.asCharBuffer());
    JsonMutableNegotiate negotiate = gson.fromJson(reader, JsonMutableNegotiate.class);
    sessionAttributes.sessionId(negotiate.getSessionId()).timestamp(negotiate.getTimestamp())
        .flowType(negotiate.getClientFlow()).credentials(negotiate.getCredentials());
  }

  public void decodeNegotiationResponseSessionAttributes(ByteBuffer buffer,
      SessionAttributes sessionAttributes) {
    CharBufferReader reader = new CharBufferReader(buffer.asCharBuffer());
    JsonMutableNegotiationResponse response = gson.fromJson(reader, JsonMutableNegotiationResponse.class);
    sessionAttributes.sessionId(response.getSessionId()).timestamp(response.getRequestTimestamp())
        .flowType(response.getServerFlow()).credentials(response.getCredentials());
  }

  public void decodeRetransmissionSequenceRange(ByteBuffer buffer, SequenceRange range) {
    CharBufferReader reader = new CharBufferReader(buffer.asCharBuffer());
    JsonMutableRetransmission retransmission = gson.fromJson(reader, JsonMutableRetransmission.class);
    range.count(retransmission.getCount()).fromSeqNo(retransmission.getFromSeqNo()).timestamp(retransmission.getRequestTimestamp());
  }

  public void decodeRetransmitRequestSequenceRange(ByteBuffer buffer, SequenceRange range) {
    CharBufferReader reader = new CharBufferReader(buffer.asCharBuffer());
    JsonMutableRetransmitRequest request = gson.fromJson(reader, JsonMutableRetransmitRequest.class);
    range.count(request.getCount()).fromSeqNo(request.getFromSeqNo()).timestamp(request.getTimestamp());
  }

  public long decodeSequence(ByteBuffer buffer) {
    CharBufferReader reader = new CharBufferReader(buffer.asCharBuffer());
    JsonMutableSequence sequence = gson.fromJson(reader, JsonMutableSequence.class);
    return sequence.getNextSeqNo();
  }

  public JsonMutableEstablish encodeEstablish(byte[] sessionId, long timestamp, long heartbeatInterval,
      long nextSeqNo, byte[] credentials) {
    JsonMutableEstablish establish = new JsonMutableEstablish(bufferSupplier);
    establish.set(sessionId, timestamp, heartbeatInterval, nextSeqNo, credentials);
    return establish;
  }

  public JsonMutableEstablishmentAck encodeEstablishmentAck(byte[] sessionId, long timestamp, long heartbeatInterval,
      long nextSeqNo) {
    JsonMutableEstablishmentAck establishAck = new JsonMutableEstablishmentAck(bufferSupplier);
    establishAck.set(sessionId, timestamp, heartbeatInterval, nextSeqNo);
    return establishAck;
  }

  public JsonMutableEstablishmentReject encodeEstablishmentReject(byte[] sessionId, long timestamp,
      EstablishmentReject rejectCode, byte[] reason) {
    JsonMutableEstablishmentReject establishReject = new JsonMutableEstablishmentReject(bufferSupplier);
    establishReject.set(sessionId, timestamp, rejectCode, reason);
    return establishReject;
  }

  public JsonMutableFinishedReceiving encodeFinishedReceiving(byte[] sessionId) {
    JsonMutableFinishedReceiving finishedReceiving = new JsonMutableFinishedReceiving(bufferSupplier);
    finishedReceiving.set(sessionId);
    return finishedReceiving;
  }

  public JsonMutableFinishedSending encodeFinishedSending(byte[] sessionId, long lastSeqNo) {
    JsonMutableFinishedSending finishedSending = new JsonMutableFinishedSending(bufferSupplier);
    finishedSending.set(sessionId, lastSeqNo);
    return finishedSending;
  }

  public JsonMutableNegotiate encodeNegotiate(byte[] sessionId, long timestamp, FlowType clientFlow,
      byte[] credentials) {
    JsonMutableNegotiate negotiate = new JsonMutableNegotiate(bufferSupplier);
    negotiate.set(sessionId, timestamp, clientFlow, credentials);
    return negotiate;
  }

  public JsonMutableNegotiationReject encodeNegotiationReject(byte[] sessionId, long requestTimestamp,
      NegotiationReject rejectCode, byte[] reason) {
    JsonMutableNegotiationReject negotiationResponse = new JsonMutableNegotiationReject(bufferSupplier);
    negotiationResponse.set(sessionId, requestTimestamp, rejectCode, reason);
    return negotiationResponse;
  }

  public JsonMutableNegotiationResponse encodeNegotiationResponse(byte[] sessionId, long requestTimestamp,
      FlowType serverFlow, byte[] credentials) {
    JsonMutableNegotiationResponse negotiationResponse = new JsonMutableNegotiationResponse(bufferSupplier);
    negotiationResponse.set(sessionId, requestTimestamp, serverFlow, credentials);
    return negotiationResponse;
  }

  public JsonMutableNotApplied encodeNotApplied(long fromSeqNo, long count) {
    JsonMutableNotApplied notApplied = new JsonMutableNotApplied(bufferSupplier);
    notApplied.set(fromSeqNo, count);
    return notApplied;
  }

  public JsonMutableRetransmission encodeRetransmission(byte[] sessionId, SequenceRange range) {
    JsonMutableRetransmission retransmission = new JsonMutableRetransmission(bufferSupplier);
    retransmission.set(sessionId, range);
    return retransmission;
  }

  public JsonMutableRetransmitRequest encodeRetransmitRequest(byte[] sessionId, SequenceRange range) {
    JsonMutableRetransmitRequest retransmitRequest = new JsonMutableRetransmitRequest(bufferSupplier);
    retransmitRequest.set(sessionId, range);
    return retransmitRequest;
  }

  public JsonMutableSequence encodeSequence(long nextSeqNo) {
    JsonMutableSequence sequence = new JsonMutableSequence(bufferSupplier);
    sequence.set(nextSeqNo);
    return sequence;
  }

  public SessionMessageType getMessageType(ByteBuffer buffer) {
    CharBufferReader reader = new CharBufferReader(buffer.asCharBuffer());
    try {
      JsonObject object = parser.parse(reader).getAsJsonObject();
      String type = object.get("@type").getAsString();
      switch (type) {
        case "Establish":
          return SessionMessageType.ESTABLISH;
        case "EstablishmentAck":
          return SessionMessageType.ESTABLISHMENT_ACK;
        case "EstablishmentReject":
          return SessionMessageType.ESTABLISHMENT_REJECT;
        case "FinishedReceiving":
          return SessionMessageType.FINISHED_RECEIVING;
        case "FinishedSending":
          return SessionMessageType.FINISHED_SENDING;
        case "Negotiate":
          return SessionMessageType.NEGOTIATE;
        case "NegotiationReject":
          return SessionMessageType.NEGOTIATION_REJECT;
        case "NegotiationResponse":
          return SessionMessageType.NEGOTIATION_RESPONSE;
        case "NotApplied":
          return SessionMessageType.NOT_APPLIED;
        case "Retransmission":
          return SessionMessageType.RETRANSMISSION;
        case "RetransmitRequest":
          return SessionMessageType.RETRANSMIT_REQUEST;
        case "Sequence":
          return SessionMessageType.SEQUENCE;
        default:
          return SessionMessageType.APPLICATION;
      }
    } catch (IllegalStateException e) {
      // Not a JSON object
      return SessionMessageType.UNKNOWN;
    }
  }

  public void init(boolean isClientSession) {

  }

}
