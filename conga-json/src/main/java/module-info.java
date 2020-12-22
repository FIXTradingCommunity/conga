module conga.json {
  requires conga.common;
  requires com.google.gson;
  provides io.fixprotocol.conga.messages.spi.MessageProvider
      with io.fixprotocol.conga.json.messages.JsonMessageProvider;
}
