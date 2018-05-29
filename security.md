# Security

The application uses WebSocket over TLS. The URI schema for that is "wss".

## Keystore Management

The server is authenticated by a certificate.

### Create a server keystore with a self-signed certificate for testing

The keytool utility is distributed with the JDK.

Default store type after Java 9 is PKCS12.

* `-keyalg` is key algorithm
* `-keystore` is the file name
* `-alias` is an entry in the file
* `-storepass` is password for the keystore
* `-validity` in days
* `-keysize` overrides the default key size for the key algorithm
* `-dname` X.500 distinguished name. See [Lightweight Directory Access Protocol (LDAP): String Representation of Distinguished Names](https://tools.ietf.org/html/rfc4514)

Example:

```
keytool -genkey -keyalg RSA -keystore selfsigned.pkcs -storepass storepassword -alias conga.server -validity 3650 -keysize 2048 -dname "CN=Conga, O=FIXTradingCommunity" 
```

### List contents of a keystore

```
keytool -list -v -keystore -storepass password -keystore selfsigned.pkcs
```

### Import a certificate into a truststore for the client

The client stores the public certificate of the server in a truststore.

Various tools may be used to capture the public certificate from a running server, 
including openssl and Chrome certificate export wizard.

```
keytool -import -trustcacerts -alias conga.server -file conga-server.cer -keystore client.pkcs -storepass storepassword
```

## Keystores and TLS in Java

### Set location and password of a truststore

Set the system properties

```
-Djavax.net.ssl.trustStore=client.pkcs
-Djavax.net.ssl.trustStorePassword=storepassword
```

### Debug TLS

Set the system property `-Djavax.net.debug=ssl`
