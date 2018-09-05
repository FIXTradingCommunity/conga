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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Reads a message log file
 *
 * @author Don Mendelson
 *
 */
public class MessageLogReader implements Closeable {

  private ReadableByteChannel channel;
  private short lastEncoding;
  private Path path = null;
  private final SofhDecoder sofhDecoder = new SofhDecoder();

  public MessageLogReader(Path path) {
    this.path = Objects.requireNonNull(path);
  }

  public MessageLogReader(ReadableByteChannel channel) {
    this.channel = Objects.requireNonNull(channel);
  }

  /**
   * Close the log
   */
  public void close() throws IOException {
    channel.close();
  }

  public short getLastEncoding() {
    return lastEncoding;
  }

  /**
   * Open the log
   * 
   * @throws IOException if the log cannot be opened
   */
  public void open() throws IOException {
    if (channel == null) {
      this.channel = FileChannel.open(path, StandardOpenOption.READ);
    }
  }

  /**
   * Read one message from a message log
   * 
   * @param dst The buffer into which bytes are to be transferred
   * @return The number of bytes read, possibly zero, or {@code -1} if the channel has reached
   *         end-of-stream. Does not include a message delimiter.
   * @throws NonReadableChannelException If this channel was not opened for reading
   *
   * @throws ClosedChannelException If this channel is closed
   *
   * @throws AsynchronousCloseException If another thread closes this channel while the read
   *         operation is in progress
   *
   * @throws ClosedByInterruptException If another thread interrupts the current thread while the
   *         read operation is in progress, thereby closing the channel and setting the current
   *         thread's interrupt status
   *
   * @throws IOException If some other I/O error occurs
   * 
   */
  public int read(ByteBuffer dst) throws IOException {
    if (channel == null) {
      throw new IOException("MessageLogReader not open");
    }
    int headerBytesRead = channel.read(sofhDecoder.getBuffer());
    if (headerBytesRead == sofhDecoder.encodedLength()) {
      long messageLength = sofhDecoder.messageLength();
      lastEncoding = sofhDecoder.encoding();
      dst.clear();
      dst.limit((int) messageLength);
      int bytesRead = channel.read(dst);
      if (bytesRead != messageLength) {
        throw new IOException("Message length inconsistent with header");
      }
      return bytesRead;
    } else {
      return Math.max(0, headerBytesRead);
    }
  }

}
