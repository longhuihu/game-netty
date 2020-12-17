/*
 * Copyright 2020 Long Huihu
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.game.netty.client.websocket;

import com.game.netty.client.acceptor.AbstractClientAcceptor;
import com.game.netty.client.acceptor.ClientAcceptorDelegate;
import com.game.netty.netty.GameNettyEnv;
import com.game.netty.netty.GameNettyUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 * web socket acceptor to listener client connection
 */
@SuppressWarnings("unused")
public class ClientWebSocketAcceptor extends AbstractClientAcceptor {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(ClientWebSocketAcceptor.class);
    private final String path;
    private final boolean textMode;

    /**
     * @param port     listen port
     * @param path     web socket url path
     * @param textMode send and receive text or binary data
     * @param delegate delegate to handle acceptor events
     */
    public ClientWebSocketAcceptor(int port, String path, boolean textMode, ClientAcceptorDelegate<?> delegate) {
        super(port, delegate);
        this.path = path;
        this.textMode = textMode;
    }

    @Override
    public Channel start() throws InterruptedException {
        SSLContext sslContext = delegate.createSSLContext();

        WebSocketFrameEncoder<?> encoder = new WebSocketFrameEncoder<>(codecConfig.getSupplier().get(), codecConfig.getClientMessageHeadSize(), textMode);

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossEventLoopGroup, workerEventLoopGroup).channel(GameNettyEnv.DEFAULT.serverChannelClass())
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) {
                        if (config.getChannelReadTimeOut() > 0) {
                            ch.pipeline().addLast(new ReadTimeoutHandler(config.getChannelReadTimeOut()));
                        }
                        // ssl
                        if (sslContext != null) {
                            SSLEngine sslEngine = sslContext.createSSLEngine();
                            sslEngine.setUseClientMode(false);
                            sslEngine.setNeedClientAuth(false);
                            ch.pipeline().addLast("ssl", new SslHandler(sslEngine));
                        }
                        if (GameNettyUtil.getLogLevel() != null) {
                            ch.pipeline().addLast(new LoggingHandler(GameNettyUtil.getLogLevel()));
                        }
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(65536));
                        ch.pipeline().addLast(new WebSocketServerProtocolHandler(path, null, true));

                        ch.pipeline().addLast(encoder);
                        ch.pipeline().addLast(new WebSocketFrameDecoder<>(codecConfig.getSupplier().get(), codecConfig.getClientMessageHeadSize(), codecConfig.isDecoderKeepMessageBuf()));
                        ch.pipeline().addLast(new ClientChannelHandler());
                    }
                }).option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.AUTO_READ, true).childOption(ChannelOption.SO_LINGER, 0);

        ChannelFuture f = b.bind(port).sync();
        Channel listenerChannel = f.channel();

        logger.info("web acceptor listener on port {}", port);
        delegate.onAcceptorStarted();
        return listenerChannel;
    }
}
