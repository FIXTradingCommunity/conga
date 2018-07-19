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

  public static abstract class Builder<T extends SbeSession, B extends SbeSession.Builder<T, B>> extends Session.Builder<T, B> {

    protected Builder() {
      this.sessionMessenger(new SbeSessionMessenger());
    }
    
    public abstract T build();

  }

  protected SbeSession(@SuppressWarnings("rawtypes") SbeSession.Builder<? extends SbeSession, ? extends SbeSession.Builder> builder) {
    super(builder);
  }

}
