module conga.client {
	requires conga.common;
	requires jdk.incubator.httpclient;
	uses io.fixprotocol.conga.messages.spi.MessageProvider;
}