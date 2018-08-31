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

package io.fixprotocol.conga.buffer;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Don Mendelson
 *
 */
public class BufferPoolTest {

  private BufferPool pool;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    pool = new BufferPool();
  }

  @Test
  public void testGet() {
    for (int i = 0; i < pool.size() + 5; i++) {
      BufferSupplier.BufferSupply supply = pool.get();
      ByteBuffer buffer = supply.acquire();
      buffer.put("abcdefg".getBytes());
      supply.release();
    }
  }
  
  @Test
  public void hold() {
    ArrayList<BufferSupplier.BufferSupply> list = new ArrayList<>();
    final int poolSize = pool.size();
    for (int i = 0; i < poolSize + 5; i++) {
      list.add(pool.get());
    }
    assertEquals(poolSize + 5, list.size());
    for (BufferSupplier.BufferSupply supply : list) {
      ByteBuffer buffer = supply.acquire();
      buffer.put("abcdefg".getBytes());
      supply.release();
      assertEquals(0, buffer.position());
    }
  }

}
