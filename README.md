![build](https://github.com/klinamen/nio-chat/workflows/build/badge.svg)
[![codecov](https://codecov.io/gh/klinamen/nio-chat/branch/master/graph/badge.svg)](https://codecov.io/gh/klinamen/nio-chat)

# Introduction
This is an example of how Java non-blocking IO API (java.nio) can be used to build a basic chat server. Once running, it will bind to all the available interfaces and listen for connections on the TCP port 10000.

You can join the chat room with a Telnet client as follows.

```
telnet localhost 10000
```

# Building and Running

```
mvn package
java -jar target/nio-chat-server-1.0-SNAPSHOT.jar
```
