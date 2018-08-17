module conga.server {
	requires conga.common;
	requires websocket.server;
	requires jetty.http;
	requires jetty.server;
	requires websocket.api;
	requires jetty.servlet;
	requires jetty.util;
	requires websocket.servlet;
	requires commons.cli;
	// expose callback methods
	exports io.fixprotocol.conga.server.io.callback;
    uses io.fixprotocol.conga.messages.spi.MessageProvider;
}