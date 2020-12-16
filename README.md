game-netty是一个使用TCP协议的网络通信开源库，它内部使用了Netty(4.1.50)，让使用者不必深入理解Netty也能轻松地编写TCP通信程序。

功能特点：

* 支持tcp通信，屏蔽了Netty的复杂性，尤其是ByteBuf的使用；
* 支持消息加密和压缩，内置了RC4加密和GZIP压缩功能，使用者可扩展；
* 支持websocket；
* 支持Client—Proxy—Server三层网络架构，在Proxy端做了大量性能优化；

## 用途

顾名思义，game-netty最适用的场景是使用TCP或WebSocket的网络游戏，它原本就是脱胎于游戏项目；后续也广泛用于服务进程之间的通信，尤其当你的业务场景简单，不想依赖额外的RPC，MQ框架时。
总之，那些需要频繁传递网络消息的场景，game-netty都可能派上用场。

## 概念

game-netty内的概念尽量与Netty保持一致，比如，xxxAcceptor代表TCP监听端，xxxConnector代表TCP连接的发起端，xxxChannel代表一条TCP连接（本质是Netty Channel的包装器）。
xxxCodec代表编解码器，消息的编码和解码逻辑一般紧密关联，所以game-netty不再将它们分开。

game-netty功能类大体被分成了Client和Proxy两组，Client意指和客户端相关，用来支持客户端发起连接(Connector)，服务器监听客户端连接请求(Acceptor)。
Proxy意指代理服务器，游戏行业也经常称为网关(Gate)服务器，包括代理连接后端服务的Connector和后端接受连接的Acceptor。
Proxy的存在直接支持了网络游戏常见的Client—Proxy—Server三层网络架构。

如果客户端直接连接服务器，项目使用game-netty的方式如下：

```
       -------------------------                     -------------------------
       |   ClientConnector     | <--ClientMessage--> |  ClientSocketAcceptor  | 
       | (ClientConnectChannel)|                     | (ClientAcceptedChannel)|
       ------------------------                      --------------------------
           Client Side                                      Server Side
```

如果采用Client—Proxy—Server架构，项目使用game-netty的方式如下：

```
       -------------------------                     ------------------------          
       |   ClientConnector     | <--ClientMessage--> |   ClientSocketAcceptor |
       | (ClientConnectChannel)|                     | (ClientAcceptedChannel)|          
       ------------------------                      |                        |                    ------------------------
                                                     |       ProxyAcceptor    | <--ProxyMessage--> |    ProxyAcceptor      | 
                                                     | (ProxyAcceptedChannel) |                    |(ProxyAcceptedChannel) |
                                                      -------------------------                    -------------------------
            Client Side                                       Proxy Side                                   Server Side
```

如果客户端和服务端使用websocket，将ClientSocketAcceptor替换成ClientWebSocketAcceptor即可。

## 消息，编解码，加密，压缩

客户端核和服务端通信的消息结构是ClientMessage，它包含三个字段：

* head：消息头，long类型(最大8字节，用户可选择0，2，4，8)  
消息头对三层架构的Proxy有意义，Proxy可以通过head得到必要的信息，从而避免对消息体解码，提高性能。
* buf：消息体的字节形式  
此字段也是为Proxy而设计，如果Proxy不需要消息体解码，可以直接转发此buf到Server端。
* body：消息体的解码后的形式   
body的类型及其编解码方式由使用者定义。

Proxy与Server之间的通信消息结构是ProxyMessage，它包含两部分：ClientMessage和ProxyHead。
ClientMessage是从Proxy从客户端接收的消息，而ProxyHead则是Proxy添加的额外信息，就像Http代理会向转发的Http请求插入额外头部字段一样。
ProxyHead没有强制的类型需求，简单情况下String也行。

game-netty对ClientMessage的编码格式已经做了规定：[length(4字节)]+[head(0~8字节)]+[body]，其中length部分是消息完整编码长度（包含自身）。
这个编码格式game-netty已经实现，使用者只需要实现body部分的编解码即可，对应接口是MessageBodyCodec。

ProxyMessage的编码格式也是类似的， [length(4字节)]+[proxy head(0~8字节)]+[ClientMessage]，其中length部分是消息完整编码长度（包含自身）。
同样，使用者只需要实现ProxyHead部分的编解码即可，对应接口是ProxyHeaderCodec。

game-netty通过BodyTransformer机制支持对消息的加密、压缩，使用者可以添加一个或多个BodyTransformer，对消息体做任意转换。

## 会话

game-netty支持会话（SessionInterface接口），所谓Session，是用来标记客户端与服务端之间的一个Channel，使用者可以在Session上附加额外的信息；
在Client—Proxy—Server三层架构中，Server可以通过Session Id来告诉Proxy：消息要发给哪个客户端。

## 配置

game-netty的配置分成两类：

* ChannelConfig：本质就是Netty Channel Option的封装；
* CodecConfig：编解码相关配置，比如ClientMessage头部尺寸，BodyTransformer列表等。

## 示例

com.game.netty.sample下有三个示例：

* socket：最简单的C/S模式，运行ClientStarter（可多个实例）和ServerStarter即可；
* websocket：socket的websocket版本；
* proxy：Client—Proxy—Server模式，需同时运行ClientStarter（可多个实例），ProxyStarter，LogicServerStarter；

>如果通过Intellij导入运行示例，需要配置Run Configuration->选中include dependencies with provide scope。

## 使用

建议maven导入，参考示例使用.

```
<dependency>
    <groupId>com.longhuihu</groupId>
    <artifactId>game-netty</artifactId>
    <version>1.0</version>
</dependency>
```

## 联系人

longhuihu@126.com












