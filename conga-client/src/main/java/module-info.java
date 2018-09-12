module conga.client {
	requires conga.common;
	requires jdk.incubator.httpclient;
	requires commons.cli;
	uses io.fixprotocol.conga.messages.spi.MessageProvider;
}