module conga.server {
	exports io.fixprotocol.conga.server.io.callback;

	requires commons.cli;
	requires conga.common;
	requires javax.servlet.api;
	requires org.eclipse.jetty.http;
	requires org.eclipse.jetty.server;
	requires org.eclipse.jetty.servlet;
	requires org.eclipse.jetty.util;
	requires org.eclipse.jetty.websocket.api;
	requires org.eclipse.jetty.websocket.servlet;
    uses io.fixprotocol.conga.messages.spi.MessageProvider;
}