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
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NonWritableChannelException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Writes messages to a log asynchronously.
 * <p>
 * Messages are delimited by FIX Simple Open Framing Header.
 * Messages are appended to an existing file.
 * 
 * @author Don Mendelson
 *
 */
public class MessageLogWriter implements Closeable {

  private AsynchronousFileChannel channel;
  private Path path = null;
  private final SofhEncoder sofhEncoder = new SofhEncoder();
  private final AtomicLong position = new AtomicLong();
  private final Consumer<Throwable> errorListener;
  private final CompletionHandler<Integer, ByteBuffer> completionHandler = new CompletionHandler<>() {

    @Override
    public void completed(Integer result, ByteBuffer attachment) {
      // do nothing on success
    }

    @Override
    public void failed(Throwable exc, ByteBuffer attachment) {
      errorListener.accept(exc);
    }
    
  };


  /**
   * Constructor with an existing channel
   * @param channel existing channel
   */
  public MessageLogWriter(AsynchronousFileChannel channel, Consumer<Throwable> errorListener) {
    this.channel = Objects.requireNonNull(channel);
    this.errorListener = Objects.requireNonNull(errorListener);
  }

  /**
   * Constructor to log to file
   * 
   * @param path file path
   * @throws IOException if the file cannot be opened
   */
  public MessageLogWriter(Path path, Consumer<Throwable> errorListener) {
    this.path = Objects.requireNonNull(path);
    this.errorListener = Objects.requireNonNull(errorListener);
  }
  
  /**
   * Close the log
   */
  public void close() throws IOException {
    if (channel != null) {
      channel.close();
    }
  }

  /**
   * Open the log
   * @throws IOException if the log cannot be opened
   */
  public void open() throws IOException {
    if (channel == null) {
      path.getParent().toFile().mkdirs();
      this.channel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
    }
    position.set(channel.size());
  }

  /**
   * Write a message to a log
   * 
   * @param buffer holds a message
   * @param encodingCode SOFH encoding type
   * @return The number of bytes written, possibly zero, not included a message delimiter.
   * @throws NonWritableChannelException If this channel was not opened for writing
   */
  public long write(ByteBuffer buffer, short encodingCode) {
    final int bytesToWrite = buffer.remaining();
    sofhEncoder.encode(bytesToWrite, encodingCode);
    long currentPosition = position.getAndAdd(bytesToWrite + sofhEncoder.encodedLength());
     channel.write(sofhEncoder.getBuffer(), currentPosition, sofhEncoder.getBuffer(), completionHandler);
     channel.write(buffer, currentPosition+sofhEncoder.encodedLength(), buffer, completionHandler);
     return bytesToWrite;
  }

}
