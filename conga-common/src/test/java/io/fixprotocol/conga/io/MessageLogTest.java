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

package io.fixprotocol.conga.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Don Mendelson
 *
 */
public class MessageLogTest {

  private MessageLogWriter writer;
  private MessageLogReader reader;
  private final short testEncoding = (short) 0xffff;
  private final Path path = FileSystems.getDefault().getPath("target/test", "test.log");
 
  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  public void setUp() throws Exception {
    try {
      FileChannel channel = FileChannel.open(path, StandardOpenOption.APPEND);
      channel.truncate(0);
      channel.close();
    } catch (IOException e) {

    }

    writer = new MessageLogWriter(path, true);
    reader = new MessageLogReader(path);
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterEach
  public void tearDown() throws Exception {
    if (writer != null) {
      writer.close();
    }
    if (reader != null) {
      reader.close();
    }
  }

  @Test
  public void testWrite() throws IOException, InterruptedException, ExecutionException {
    writer.open();
    byte [][] srcs = new byte [][] {"abcdefghijklm".getBytes(), "nopqrstuvwxyz".getBytes(),
      "0123456789".getBytes()};
    for (byte [] src : srcs) {
      ByteBuffer in = allocateBuffer();
      in.put(src);
      in.flip();
      assertEquals(src.length, writer.writeAsync(in, testEncoding).get().intValue());
    }
    writer.close();
    reader.open();
    for (byte [] src : srcs) {
      ByteBuffer out = allocateBuffer();
      assertEquals(src.length, reader.read(out));
      byte[] dst = new byte[src.length];
      out.flip();
      out.get(dst);
      assertArrayEquals(src, dst);
    }
    ByteBuffer out = allocateBuffer();
    assertTrue(reader.read(out) <= 0);
  }


  private ByteBuffer allocateBuffer() {
    return ByteBuffer.allocate(1024).order(ByteOrder.nativeOrder());
  }

}
