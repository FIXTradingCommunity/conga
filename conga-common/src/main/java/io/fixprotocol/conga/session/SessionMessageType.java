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
 * The type of messages that may be sent and received on a FIXP session
 */
public enum SessionMessageType {
  /**
   * A new application message
   */
  APPLICATION,
  /**
   * Establish request message
   */
  ESTABLISH,
  /**
   * Establishment response message
   */
  ESTABLISHMENT_ACK,
  /**
   * Establishment rejection message
   */
  ESTABLISHMENT_REJECT,
  /**
   * Finalize session response
   */
  FINISHED_RECEIVING,
  /**
   * Finalize session request
   */
  FINISHED_SENDING,
  /**
   * Negotiate request message
   */
  NEGOTIATE,
  /**
   * Negotiation rejected
   */
  NEGOTIATION_REJECT,
  /**
   * Negotiation accepted
   */
  NEGOTIATION_RESPONSE,
  /**
   * Notification that one or more messages were missed
   */
  NOT_APPLIED,
  /**
   * A retransmitted application message
   */
  RETRANSMISSION,
  /**
   * A request to retransmit one or more missed messages
   */
  RETRANSMIT_REQUEST, /**
   * Sequence message, used as a heartbeat
   */
  SEQUENCE, /**
   * Unknown message type
   */
  UNKNOWN
}