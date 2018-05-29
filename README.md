# conga

This project demonstrates high performance FIX using Web and FIX protocols.

### Participation

FIX Trading Community welcomes participation in this project by any interested parties.

## The Protocol Stack

### Application Layer

For demonstration purposes, the project provides a simple order match engine and a trader client to inject orders.

Application messages convey FIX semantics. See [FIX Application Level](https://www.fixtrading.org/standards/).

### Presentation Layer

Messages are encoded in [Simple Binary Encoding](https://github.com/FIXTradingCommunity/fix-simple-binary-encoding), a FIX standard. The implementation is very low latency due to its use of native binary data types and deterministic message layouts controlled by templates.

Message framing is performed by [WebSocket protocol](https://tools.ietf.org/html/rfc6455).

### Session Layer

Sessions are initiated as an HTTP request to upgrade to WebSocket. WebSocket, an IETF protocol, provides two-way communication, unlike HTTP itself.

### Transport Layer

WebSocket protocol runs over a TCP transport, which provides basic reliability of message delivery, and it uses Ping/Pong messages as keepalives.

### Security

As part of the initial HTTP contact, a TLS handshake negotiates cipher suites and performs authentication. The server is authenticated by a certificate.

## Planned Enhancements

* Use alternative encodings, e.g. Google Protocol Buffers, or JSON (not high performance)
* Enhanced client authentication
* Message idempotency and recoverability using [FIX Performance Session Layer](https://github.com/FIXTradingCommunity/fixp-specification) semantics. FIXP is a FIX protocol.

## Prerequisites

### Java
This project requires Java 10 or later. It should run on any platform for which the JVM is supported.

The project uses HTTPClient in module `jdk.incubator.httpclient` that is distributed with the JDK. Since it is still an incubator module, the module and package names will definitely change in the future.

The server implementation of WebSocket is provided by Jetty.

### Build
The project is built with Maven version 3.0 or later. 

## License
© Copyright 2018 FIX Protocol Limited

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


