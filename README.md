## 一、用途

GameNetty是一个使用TCP协议的网络通信开源库，它内部使用了Netty(4.1.50)，让使用者不必深入理解Netty也能轻松地编写TCP通信程序。

功能特点：

* 支持tcp通信，屏蔽了Netty的复杂性，尤其是ByteBuf的使用；
* 灵活的消息格式，使用者可自由决定使用二进制或文本消息格式；
* 支持消息加密和压缩，内置了RC4加密和GZIP压缩功能，使用者可扩展；
* 支持websocket；
* 实现了Client—Proxy—Server(客户端—游戏网关—游戏服）三层网络结构，在Proxy端做了大量性能优化；

一句话总结，使用GameNetty来构建TCP服务，就像使用Spring MVC构建基于HTTP服务一样简单


GameNetty最适用的场景是使用TCP或WebSocket的网络游戏，它原本就是脱胎于游戏项目；后续也广泛用于服务进程之间的通信，尤其当你的业务场景简单，不想依赖额外的RPC，MQ框架时。总之，那些需要频繁传递网络消息的场景，GameNetty都可能派上用场。

## 二、概念

GameNetty内的概念尽量与Netty保持一致，比如，xxxAcceptor代表TCP监听端，xxxConnector代表TCP连接的发起端，xxxChannel代表一条TCP连接（本质是Netty Channel的包装器）。
xxxCodec代表编解码器，消息的编码和解码逻辑一般紧密关联，所以GameNetty不再将它们分开。

GameNetty功能类大体被分成了Client和Proxy两组，Client意指和客户端相关，用来支持客户端发起连接(Connector)，服务器监听客户端连接请求(Acceptor)。

Proxy意指代理服务器，游戏行业也经常称为网关(Gate)服务器，包括代理连接后端服务的Connector和后端接受连接的Acceptor。Proxy的存在直接支持了网络游戏常见的Client—Proxy—Server三层网络结构。

如果客户端直连服务器，项目使用GameNetty的方式如下：

```
       -------------------------                     -------------------------
       |   ClientConnector     | <--ClientMessage--> |  ClientSocketAcceptor  | 
       | (ClientConnectChannel)|                     | (ClientAcceptedChannel)|
       ------------------------                      --------------------------
           Client Side                                      Server Side
```

如果采用Client—Proxy—Server结构，项目使用GameNetty的方式如下：

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

如果使用websocket，将ClientSocketAcceptor替换成ClientWebSocketAcceptor即可。

## 三、消息，编解码，加密，压缩

### ClientMessage

GameNetty里，客户端核和服务端之间的通信消息被抽象为ClientMessage，它包含三个字段：

* head—消息头 

最大8字节，用户可选择0，1，2，4，8，因此简单起见，定义为long类型。
消息头的存在对三层结构的Proxy有重要意义，Proxy可以通过head得到必要的信息，而避免对消息体解码；

* buf—消息体ByteBuf  

此字段用于Proxy性能优化，Proxy可以直接转发buf，从而避免对消息重新编码；

* body—消息体对象  

它是消息体解码后的数据对象，Proxy端可能不需要对所有消息解析出body，在Server端，一般都需要解析body。

### ProxyMessage

Proxy与Server之间的通信消息被抽象为ProxyMessage，它包含两部分：ClientMessage和ProxyHead。

ClientMessage是从客户端接收的消息，而ProxyHead则是Proxy添加的额外信息，就像Http代理会向Http请求插入额外头部字段一样。

ProxyHead没有强制的类型需求，简单情况下String就足够。

### 编码

GameNetty对ClientMessage的编码格式已经做了规定：[length(4字节)]+[head(0~8字节)]+[body]，其中length部分是消息完整编码长度（包含自身）。
body部分的编解码由使用者来实现，对应接口是MessageBodyCodec。

ProxyMessage的编码格式也是类似的， [length(4字节)]+[proxy head(0~8字节)]+[ClientMessage]，其中length部分是消息完整编码长度（包含自身）。
ProxyHead的部分的编解码由使用者实现，对应接口是ProxyHeaderCodec。

### 加密，压缩

GameNetty通过BodyTransformer机制支持对消息的加密、压缩，用户可以添加一个或多个BodyTransformer，对消息体做任意转换。

GameNetty内置了RC4BodyEnDecryptor，GzipBody(Un)Compressor等BodyTransformer。

## 四、会话

GameNetty支持会话（SessionInterface接口），所谓Session，是用来标记客户端与服务端之间的一个Channel，使用者可以在Session上附加额外的信息；
在Client—Proxy—Server三层架构中，Session对象在Proxy上，session id具备全局唯一性，Server通过session id来穿透Proxy与Client通信。

## 五、示例

运行示例是上手GameNetty最快的方式，com.game.netty.sample下有三个示例，这些示例的功能是类似的：客户端向服务端发送一个消息，服务端返回一个响应，
并且有一定概率在几秒种后向客户端主动推送一个消息。

* socket

模拟最简单的C/S模式，运行ClientStarter（客户端，可多个实例）和ServerStarter(服务端)即可；

* websocket

socket的websocket版本；

* proxy

模拟Client—Proxy—Server结构，需同时运行ClientStarter（客户端，可多个实例），ProxyStarter(代理端），LogicServerStarter(服务端)；
该示例展示了Proxy服务如何转发Client消息到指定的LogicServer，而后者可以通过session与指定Client通信。

>如果通过Intellij导入代码，运行示例，需要配置Run Configuration->选中include dependencies with provide scope。

## 六、使用

建议maven导入，参考示例使用.

```
<dependency>
    <groupId>com.github.longhuihu</groupId>
    <artifactId>game-netty</artifactId>
    <version>1.0</version>
</dependency>
```

## 七、联系人

longhuihu@126.com

个人博客：https://blog.csdn.net/longhuihu
