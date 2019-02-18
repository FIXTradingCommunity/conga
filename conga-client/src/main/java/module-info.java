module conga.client {

	requires commons.cli;
	requires conga.common;
	requires java.net.http;
	uses io.fixprotocol.conga.messages.spi.MessageProvider;
}