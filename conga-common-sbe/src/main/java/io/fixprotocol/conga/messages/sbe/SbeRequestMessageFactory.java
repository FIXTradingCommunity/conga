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

package io.fixprotocol.conga.messages.sbe;

import java.nio.ByteBuffer;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import io.fixprotocol.conga.messages.Message;
import io.fixprotocol.conga.messages.MessageException;
import io.fixprotocol.conga.messages.RequestMessageFactory;
import io.fixprotocol.conga.sbe.messages.appl.MessageHeaderDecoder;
import io.fixprotocol.conga.sbe.messages.appl.NewOrderSingleDecoder;
import io.fixprotocol.conga.sbe.messages.appl.OrderCancelRequestDecoder;

/**
 * @author Don Mendelson
 *
 */
public class SbeRequestMessageFactory implements RequestMessageFactory {

  private static final ThreadLocal<DirectBuffer> bufferThreadLocal =
      ThreadLocal.withInitial(UnsafeBuffer::new);

  private final ThreadLocal<SbeOrderCancelRequest> cancelRequestThreadLocal =
      ThreadLocal.withInitial(SbeOrderCancelRequest::new);

  private final ThreadLocal<MessageHeaderDecoder> headerThreadLocal =
      ThreadLocal.withInitial(MessageHeaderDecoder::new);

  private final ThreadLocal<SbeNewOrderSingle> newOrderSingleThreadLocal =
      ThreadLocal.withInitial(SbeNewOrderSingle::new);

  @Override
  public SbeNewOrderSingle getNewOrderSingle() {
    return newOrderSingleThreadLocal.get();
  }

  @Override
  public SbeOrderCancelRequest getOrderCancelRequest() {
    return cancelRequestThreadLocal.get();
  }


  @Override
  public Message wrap(ByteBuffer buffer) throws MessageException {
    final DirectBuffer directBuffer = bufferThreadLocal.get();
    directBuffer.wrap(buffer);
    final var messageHeaderDecoder = headerThreadLocal.get();
    final int offset = buffer.position();
    messageHeaderDecoder.wrap(directBuffer, offset);
    int blockLength = messageHeaderDecoder.blockLength();
    int templateId = messageHeaderDecoder.templateId();
    int schemaId = messageHeaderDecoder.schemaId();
    int schemaVersion = messageHeaderDecoder.version();

    if (NewOrderSingleDecoder.SCHEMA_ID != schemaId) {
      throw new MessageException("Unknown message schema");
    }

    switch (templateId) {
      case NewOrderSingleDecoder.TEMPLATE_ID:
        final var newOrderSingle = getNewOrderSingle();
        newOrderSingle.wrap(buffer, offset + messageHeaderDecoder.encodedLength(), blockLength,
            schemaVersion);
        return newOrderSingle;
      case OrderCancelRequestDecoder.TEMPLATE_ID:
        var orderCancelRequest = getOrderCancelRequest();
        orderCancelRequest.wrap(buffer, offset + messageHeaderDecoder.encodedLength(), blockLength,
            schemaVersion);
        return orderCancelRequest;
      default:
        throw new MessageException("Unknown template");
    }
  }

}
