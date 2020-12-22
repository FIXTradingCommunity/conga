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

package io.fixprotocol.conga.sbe.messages.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fixprotocol.conga.messages.appl.MutableMessage;
import io.fixprotocol.conga.session.FlowType;
import io.fixprotocol.conga.session.SessionAttributes;
import io.fixprotocol.conga.session.SessionMessageType;

/**
 * @author Don Mendelson
 *
 */
public class SbeSessionMessengerTest {

  private SbeSessionMessenger client;
  private SbeSessionMessenger server;

  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  public void setUp() throws Exception {
    client = new SbeSessionMessenger();
    client.init(true);
    server = new SbeSessionMessenger();
    server.init(false);
  }

  @Test
  public void establish() throws Exception {
    byte[] sessionId = UUIDAsBytes(UUID.randomUUID());
    long timestamp = System.nanoTime();
    byte[] credentials = null;
    long heartbeatInterval = 250;
    long nextSeqNo = 99;
    MutableMessage mutableEstablish =
        client.encodeEstablish(sessionId, timestamp, heartbeatInterval, nextSeqNo, credentials);
    ByteBuffer buffer = mutableEstablish.toBuffer();
    SessionMessageType type = server.getMessageType(buffer.duplicate().order(buffer.order()));
    assertEquals(SessionMessageType.ESTABLISH, type);
    SessionAttributes sessionAttributes = new SessionAttributes();
    server.decodeEstablishSessionAttributes(buffer, sessionAttributes);
    assertEquals(nextSeqNo, sessionAttributes.getNextSeqNo());
    mutableEstablish.release();
  }

  @Test
  public void negotiate() throws Exception {
    byte[] sessionId = UUIDAsBytes(UUID.randomUUID());
    long timestamp = System.nanoTime();
    FlowType clientFlow = FlowType.Idempotent;
    byte[] credentials = null;
    MutableMessage mutableNegotiate =
        client.encodeNegotiate(sessionId, timestamp, clientFlow, credentials);
    ByteBuffer buffer = mutableNegotiate.toBuffer();
    SessionMessageType type = server.getMessageType(buffer.duplicate().order(buffer.order()));
    assertEquals(SessionMessageType.NEGOTIATE, type);
    SessionAttributes sessionAttributes = new SessionAttributes();
    server.decodeNegotiateSessionAttributes(buffer, sessionAttributes);
    assertEquals(clientFlow, sessionAttributes.getFlowType());
    mutableNegotiate.release();
  }

  @Test
  public void sequence() throws Exception {
    long nextSeqNo = 99;
    MutableMessage mutableSequence =
        client.encodeSequence(nextSeqNo );
    ByteBuffer buffer = mutableSequence.toBuffer();
    SessionMessageType type = server.getMessageType(buffer.duplicate().order(buffer.order()));
    assertEquals(SessionMessageType.SEQUENCE, type);
    assertEquals(nextSeqNo, server.decodeSequence(buffer));
    mutableSequence.release();
  }
  
  private static byte[] UUIDAsBytes(UUID uuid) {
    Objects.requireNonNull(uuid);
    final byte[] sessionId = new byte[16];
    // UUID is big-endian according to standard, which is the default byte
    // order of ByteBuffer
    final ByteBuffer b = ByteBuffer.wrap(sessionId);
    b.putLong(0, uuid.getMostSignificantBits());
    b.putLong(8, uuid.getLeastSignificantBits());
    return sessionId;
  }

}
