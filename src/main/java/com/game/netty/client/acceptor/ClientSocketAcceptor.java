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

package com.game.netty.client.acceptor;

import com.game.netty.netty.GameNettyEnv;
import com.game.netty.netty.GameNettyUtil;
import com.game.netty.client.codec.ClientMessageDecoder;
import com.game.netty.client.codec.ClientMessageEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * tcp socket acceptor to listener client connection
 */
public class ClientSocketAcceptor extends AbstractClientAcceptor {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(ClientSocketAcceptor.class);

    /**
     * the encoder is stateless, shared by all channel
     */
    private ClientMessageEncoder<?> sharedEncoder;


    /**
     * @param port     listen port
     * @param delegate delegate to handle acceptor events
     */
    public ClientSocketAcceptor(int port, ClientAcceptorDelegate<?> delegate) {
        super(port, delegate);
    }

    /**
     *  the user can use the encoder to encode ClientMessage to ByteBuf outside netty pipeline
     * @see ClientMessageEncoder
     */
    public ClientMessageEncoder<?> getSharedEncoder() {
        return sharedEncoder;
    }

    @Override
    public Channel start() throws InterruptedException {

        sharedEncoder = new ClientMessageEncoder<>(codecConfig.getSupplier().get(), codecConfig.getClientMessageHeadSize());
        codecConfig.getBodyEncodeTransformers().forEach(sharedEncoder::addTransformer);

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossEventLoopGroup, workerEventLoopGroup).channel(GameNettyEnv.DEFAULT.serverChannelClass())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (config.getChannelReadTimeOut() > 0) {
                            pipeline.addLast(new ReadTimeoutHandler(config.getChannelReadTimeOut()));
                        }
                        if (GameNettyUtil.getLogLevel() != null) {
                            pipeline.addLast(new LoggingHandler(GameNettyUtil.getLogLevel()));
                        }
                        pipeline.addLast(sharedEncoder);
                        ClientMessageDecoder<?> clientMessageDecoder = new ClientMessageDecoder<>(codecConfig.getSupplier().get(), codecConfig.getClientMessageHeadSize(), codecConfig.isDecoderKeepMessageBuf());
                        codecConfig.getBodyDecodeTransformers().forEach(clientMessageDecoder::addTransformer);
                        pipeline.addLast(clientMessageDecoder);
                        pipeline.addLast(new ClientChannelHandler());
                    }
                });

        config.getChannelOptions().forEach(b::childOption);

        ChannelFuture f = b.bind(port).sync();
        Channel listenerChannel = f.channel();
        logger.info("socket acceptor listener on port {}", port);

        bossEventLoopGroup.execute(delegate::onAcceptorStarted);

        return listenerChannel;
    }
}
