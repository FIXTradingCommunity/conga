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

/**
 * Attributes to establish a session
 * 
 * @author Don Mendelson 
 *
 */
public class SessionAttributes {
  private byte[] credentials;
  private FlowType flowType;
  private long keepAliveInterval;
  private long nextSeqNo;
  private byte[] sessionId = new byte [16];
  private long timestamp;

  public SessionAttributes credentials(byte[] credentials) {
    this.credentials = credentials;
    return this;
  }

  public SessionAttributes flowType(FlowType flowType) {
    this.flowType = flowType;
    return this;
  }

  public byte[] getCredentials() {
    return credentials;
  }
  
  public FlowType getFlowType() {
    return flowType;
  }

  public long getKeepAliveInterval() {
    return keepAliveInterval;
  }

  public long getNextSeqNo() {
    return nextSeqNo;
  }

  public byte[] getSessionId() {
    return sessionId;
  }
  
  public long getTimestamp() {
    return timestamp;
  }

  public SessionAttributes keepAliveInterval(long keepAliveInterval) {
    this.keepAliveInterval = keepAliveInterval;
    return this;
  }

  public SessionAttributes nextSeqNo(long nextSeqNo) {
    this.nextSeqNo = nextSeqNo;
    return this;
  }

  public SessionAttributes sessionId(byte[] sessionId) {
    this.sessionId = sessionId;
    return this;
  }

  public SessionAttributes timestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

}
