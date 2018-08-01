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

package io.fixprotocol.conga.messages.appl;

/**
 * Failure to parse or construct a message
 * 
 * @author Don Mendelson
 *
 */
public class MessageException extends Exception {

  private static final long serialVersionUID = -3299111482659075941L;

  /**
   * 
   */
  public MessageException() {
  }

  /**
   * @param message
   */
  public MessageException(String message) {
    super(message);
  }

  /**
   * @param cause
   */
  public MessageException(Throwable cause) {
    super(cause);
  }

  /**
   * @param message
   * @param cause
   */
  public MessageException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * @param message
   * @param cause
   * @param enableSuppression
   * @param writableStackTrace
   */
  public MessageException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
