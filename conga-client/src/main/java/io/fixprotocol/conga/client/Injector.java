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

package io.fixprotocol.conga.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.fixprotocol.conga.buffer.BufferSupplier.BufferSupply;
import io.fixprotocol.conga.io.LogMessageSupplier;
import io.fixprotocol.conga.messages.appl.ApplicationMessageConsumer;
import io.fixprotocol.conga.messages.appl.Message;

/**
 * @author Don Mendelson
 *
 */
public class Injector extends Trader implements Runnable {

  public static class Builder extends Trader.Builder<Injector, Builder> {

    public static final String DEFAULT_INPUT_PATH = "data";

    private String inputPath = DEFAULT_INPUT_PATH;
    private int waitSeconds = 0;
    private int batches = 1;

    @Override
    public Injector build() throws URISyntaxException {
      super.build();
      return new Injector(this);
    }

    public Builder batches(int batches) {
      this.batches = batches;
      return this;
    }

    public Builder inputPath(String path) {
      this.inputPath = Objects.requireNonNull(path);
      return this;
    }

    public Builder waitSeconds(int waitSeconds) {
      this.waitSeconds = waitSeconds;
      return this;
    }

  }

  public static Builder builder() {
    return new Builder();
  }

  private static class DefaultMessageConsumer implements ApplicationMessageConsumer {


    @Override
    public void accept(String source, Message message, long seqNo) {
      // by default do nothing, rely on message log
    }
  }

  public static void main(String[] args) throws Exception {

    final Builder builder = Injector.builder();
    buildFromArgs(builder, args);
    try (Injector injector = builder.messageListener(new DefaultMessageConsumer()).build()) {
      injector.open();
      injector.run();
    }
  }

  private static void buildFromArgs(Builder builder, String[] args) {
    Options options = new Options();
    options.addOption("i", "input", true, "path of input file");
    options.addOption("o", "output", true, "path of output file");
    options.addOption("e", "encoding", true, "message encoding");
    options.addOption("a", "apipath", true, "API path");
    options.addOption("h", "host", true, "remote host");
    options.addOption(Option.builder("p").longOpt("port").hasArg(true).desc("remote port")
        .type(Number.class).build());
    options.addOption(Option.builder("u").longOpt("uri").hasArg(true)
        .desc("API URI as wss://host.apipath:port").type(URI.class).build());
    options.addOption(Option.builder("t").longOpt("timeout").hasArg(true).desc("timeout seconds")
        .type(Number.class).build());
    options.addOption(Option.builder("k").longOpt("keepalive").hasArg(true)
        .desc("keepalive interval millis").type(Number.class).build());
    options.addOption(Option.builder("b").longOpt("batches").hasArg(true)
        .desc("number of injection batches").type(Number.class).build());
    options.addOption(Option.builder("w").longOpt("wait").hasArg(true)
        .desc("wait between injection batches in seconds").type(Number.class).build());
    options.addOption("?", "help", false, "disply usage");

    DefaultParser parser = new DefaultParser();
    CommandLine cmd;
    try {
      cmd = parser.parse(options, args);

      if (cmd.hasOption("?")) {
        usage(options);
        System.exit(0);
      }
      if (cmd.hasOption("i")) {
        String input = cmd.getOptionValue("i");
        builder.inputPath(input);
      }
      // String output = cmd.getOptionValue("o");
      // builder.outputPath(output);
      if (cmd.hasOption("e")) {
        String encoding = cmd.getOptionValue("e");
        builder.encoding(encoding);
      }
      if (cmd.hasOption("a")) {
        String apiPath = cmd.getOptionValue("a");
        builder.apiPath(apiPath);
      }
      if (cmd.hasOption("h")) {
        String host = cmd.getOptionValue("h");
        builder.remoteHost(host);
      }
      if (cmd.hasOption("p")) {
        Number port = (Number) cmd.getParsedOptionValue("p");
        builder.remotePort(port.intValue());
      }
      if (cmd.hasOption("u")) {
        URI uri = (URI) cmd.getParsedOptionValue("u");
        builder.uri(uri);
      }
      if (cmd.hasOption("t")) {
        Number timeoutSeconds = (Number) cmd.getParsedOptionValue("t");
        builder.timeoutSeconds(timeoutSeconds.intValue());
      }
      if (cmd.hasOption("k")) {
        Number keepalive = (Number) cmd.getParsedOptionValue("l");
        builder.heartbeatInterval(keepalive.longValue());
      }
      if (cmd.hasOption("b")) {
        Number batches = (Number) cmd.getParsedOptionValue("b");
        builder.batches(batches.intValue());
      }
      if (cmd.hasOption("w")) {
        Number waitSeconds = (Number) cmd.getParsedOptionValue("w");
        builder.waitSeconds(waitSeconds.intValue());
      }
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      usage(options);
      System.exit(1);
    }
  }

  private static void usage(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Injector", options);
  }

  private LogMessageSupplier messageSupplier;
  private final int batches;
  private final int waitSeconds;

  protected Injector(Builder builder) {
    super(builder);
    this.batches = builder.batches;
    this.waitSeconds = builder.waitSeconds;
    Path inputPath = FileSystems.getDefault().getPath(builder.inputPath);
    try {
      messageSupplier = new LogMessageSupplier(inputPath, getRequestBufferSupplier());
    } catch (IOException e) {
      getErrorListener().accept(e);
    }
  }

  @Override
  public void run() {
    List<BufferSupply> supplies = null;
    try {
      messageSupplier.open();
      // Allocate buffers for a batch
      supplies = new ArrayList<>();
      BufferSupply bufferSupply;
      do {
        bufferSupply = messageSupplier.get();
        if (bufferSupply != null) {
          supplies.add(bufferSupply);
        }
      } while (bufferSupply != null);

      for (int i = 0; i < batches; i++) {
        Thread.sleep(waitSeconds*1000L);
        
        // Inject a batch
        for (BufferSupply supply : supplies) {
          sendApplicationMessage(supply.acquire().duplicate());
        }
      }
    } catch (IOException | InterruptedException e) {
      getErrorListener().accept(e);
    } finally {
      // release the buffers
      if (supplies != null) {
        for (BufferSupply supply : supplies) {
          supply.release();
        }
      }
    }
    try {
      Thread.sleep(waitSeconds*1000L);
    } catch (InterruptedException e) {

    }
    suspend();
  }
}
