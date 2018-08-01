module conga.server {
	requires conga.common;
	requires websocket.server;
	requires jetty.http;
	requires jetty.server;
	requires websocket.api;
	requires jetty.servlet;
	requires jetty.util;
	requires websocket.servlet;
    uses io.fixprotocol.conga.messages.spi.MessageProvider;
}