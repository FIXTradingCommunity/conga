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

package io.fixprotocol.conga.server.match;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Strictly monotonic and deterministic time source
 * @author Don Mendelson
 *
 */
class TestClock extends Clock {

  public TestClock() {
    this(Instant.EPOCH, ZoneId.of("Z"));
  }
  
  public TestClock(Instant instant) {
    this(instant, ZoneId.of("Z"));
  }
  
  public TestClock(Instant instant, ZoneId zone) {
    this.epochMilli = instant.toEpochMilli();
    this.zone = zone;
  }

  private long epochMilli = 0;
  private final ZoneId zone;

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return new TestClock(Instant.ofEpochMilli(epochMilli), zone);
  }

  @Override
  public Instant instant() {
    return Instant.ofEpochMilli(++epochMilli );
  }
  
}