module conga.server {
	requires conga.common;
	requires commons.cli;
    requires jetty.http;
	requires jetty.server;
	requires jetty.servlet;
	requires jetty.util;
	requires websocket.api;
	requires websocket.servlet;
	// expose callback methods
	exports io.fixprotocol.conga.server.io.callback;
    uses io.fixprotocol.conga.messages.spi.MessageProvider;
}