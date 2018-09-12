module conga.common {
  requires disruptor;
  exports io.fixprotocol.conga.buffer;
  exports io.fixprotocol.conga.io;
  exports io.fixprotocol.conga.messages.appl;
  exports io.fixprotocol.conga.messages.session;
  exports io.fixprotocol.conga.messages.spi;
  exports io.fixprotocol.conga.session;
}