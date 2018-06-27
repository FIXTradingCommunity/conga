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

import io.fixprotocol.conga.messages.NotApplied;
import io.fixprotocol.conga.sbe.messages.fixp.NotAppliedDecoder;

/**
 * @author Don Mendelson
 *
 */
public class SbeNotApplied implements NotApplied, SbeMessageWrapper{
  
  private final NotAppliedDecoder decoder = new NotAppliedDecoder();
  private final DirectBuffer directBuffer = new UnsafeBuffer();
  private String source = null;


  @Override
  public long getCount() {
    return decoder.count();
  }

  @Override
  public long getFromSeqNo() {
    return decoder.fromSeqNo();
  }

  @Override
  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  @Override
  public ByteBuffer toBuffer() {
    return directBuffer.byteBuffer();
  }
  
  @Override
  public void wrap(ByteBuffer buffer, int offset, int actingBlockLength, int actingVersion) {
    directBuffer.wrap(buffer);
    decoder.wrap(directBuffer, offset, actingBlockLength, actingVersion);
  }

}
