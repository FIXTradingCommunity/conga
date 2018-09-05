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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Dumps messages from a log file in humanly readable form
 *
 * @author Don Mendelson
 *
 */
public class MessageLogDumper {

  /**
   * Usage: MessageLogDumper <log-path>
   * @param args the first argument is the log file path
   * @throws IOException if the log cannot be read
   */
  public static void main(String[] args) throws IOException {
    if (args.length >= 1) {
      Path path = FileSystems.getDefault().getPath(args[0]);
      dump(path, System.out);
    } else {
      System.err.println("Usage: MessageLogDumper <log-path>");
    }
  }

  public static void dump(Path path, PrintStream out) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(8096);
    try (MessageLogReader reader = new MessageLogReader(path)) {
      reader.open();

      while (reader.read(buffer) > 0) {
        BufferDumper.print(buffer, 16, out);
      }

    }
  }
}
