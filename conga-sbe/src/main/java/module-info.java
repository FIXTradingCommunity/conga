module conga.sbe {
  requires conga.common;
  requires org.agrona.core;
  provides io.fixprotocol.conga.messages.spi.MessageProvider
      with io.fixprotocol.conga.sbe.messages.SbeMessageProvider;
}
