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
import java.util.ListIterator;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Don Mendelson
 *
 */
public class BufferCacheTest {

  private BufferCache cache;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    cache = new BufferCache();
  }

  @Test
  public void testAddByteBuffer() {
    assertEquals(0, cache.size());
    ByteBuffer src = createBuffer("ABC");
    assertTrue(cache.add(src));
    assertEquals(1, cache.size());
    ByteBuffer dest = cache.get(0);
    assertEquals(src, dest);
  }

  @Test
  public void testAddIntByteBuffer() {
    ByteBuffer src1 = createBuffer("ABC");
    cache.add(src1);
    ByteBuffer src2 = createBuffer("DEF");
    cache.add(1, src2);
    assertEquals(2, cache.size());
    ByteBuffer dest = cache.get(1);
    assertEquals(src2, dest);
  }

  @Test
  public void testListIterator() {
    ByteBuffer src1 = createBuffer("ABC");
    cache.add(src1);
    ByteBuffer src2 = createBuffer("DEF");
    cache.add(src2);
    ByteBuffer src3 = createBuffer("GHI");
    cache.add(src3);
    ListIterator<ByteBuffer> iter = cache.listIterator();
    assertTrue(iter.hasNext());
    ByteBuffer dest = iter.next();
    String text = displayBuffer(dest);
    assertEquals(src1, dest);
    dest = iter.next();
    assertEquals(src2, dest);
    dest = iter.next();
    assertEquals(src3, dest);
    assertFalse(iter.hasNext());
    assertTrue(iter.hasPrevious());
    dest = iter.previous();
    assertEquals(src3, dest);
  }

  @Test
  public void testSet() {
    ByteBuffer src1 = createBuffer("ABC");
    cache.add(src1);
    ByteBuffer src2 = createBuffer("DEF");
    cache.add(src2);
    ByteBuffer src3 = createBuffer("GHI");
    cache.set(1, src3);
    ByteBuffer dest = cache.get(1);
    assertEquals(src3, dest);
  }

  private ByteBuffer createBuffer(String text) {
    ByteBuffer src = ByteBuffer.allocate(1024);
    src.put(text.getBytes());
    src.flip();
    return src;
  }
  
  private String displayBuffer(ByteBuffer buffer) {
    ByteBuffer dup = buffer.duplicate();
    byte[] dst = new byte[dup.remaining()];
    dup.get(dst);
    return new String(dst);
  }

}
