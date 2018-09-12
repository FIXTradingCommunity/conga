module conga.json {
  requires conga.common;
  requires gson;
  provides io.fixprotocol.conga.messages.spi.MessageProvider
      with io.fixprotocol.conga.json.messages.JsonMessageProvider;
}
