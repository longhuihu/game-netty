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

package com.game.netty.proxy.acceptor;

import com.game.netty.ChannelEvent;
import com.game.netty.proxy.ProxyMessage;
import com.game.netty.netty.GameNettyEnv;
import com.game.netty.netty.GameNettyUtil;
import com.game.netty.proxy.codec.ProxyMessageDecoder;
import com.game.netty.proxy.codec.ProxyMessageEncoder;
import com.game.netty.config.ProxyChannelConfig;
import com.game.netty.config.codec.ProxyCodecConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Acceptor at logic server for listening tcp connect from proxy
 */
public class ProxyAcceptor {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyAcceptor.class);
    private final EventLoopGroup bossEventLoopGroup;
    private final EventLoopGroup workerEventLoopGroup;
    private ProxyMessageEncoder<?, ?> sharedEncoder;

    private final Map<ChannelId, ProxyAcceptedChannel> channels = new ConcurrentHashMap<>();
    private final ProxyAcceptorDelegate<?, ?> delegate;
    private ProxyChannelConfig config;
    private ProxyCodecConfig codecConfig;
    private final int port;
    private Channel listenerChannel;


    /**
     * @param port     listening port
     * @param delegate delegator for handle acceptor related events
     */
    public ProxyAcceptor(int port, ProxyAcceptorDelegate<?, ?> delegate) {
        this.delegate = delegate;
        this.port = port;
        bossEventLoopGroup = GameNettyEnv.DEFAULT.createBossEventLoopGroup();
        workerEventLoopGroup = GameNettyEnv.DEFAULT.createWorkerEventLoopGroup(0);
    }

    /**
     * config codec
     */
    public ProxyAcceptor initCodec(ProxyCodecConfig codecConfig) {
        this.codecConfig = codecConfig;
        return this;
    }

    /**
     * config channel
     */
    public ProxyAcceptor initChannel(ProxyChannelConfig acceptorConfig) {
        this.config = acceptorConfig;
        return this;
    }

    /**
     * start listen
     */
    public void start() throws InterruptedException {
        sharedEncoder = new ProxyMessageEncoder<>(codecConfig.headCodec().get(), codecConfig.bodyCodec().get(), codecConfig.getClientMessageHeadSize());

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossEventLoopGroup, workerEventLoopGroup).channel(GameNettyEnv.DEFAULT.serverChannelClass())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        if (GameNettyUtil.getLogLevel() != null) {
                            ch.pipeline().addLast(new LoggingHandler(GameNettyUtil.getLogLevel()));
                        }
                        ch.pipeline().addLast(sharedEncoder);
                        ch.pipeline().addLast(new ProxyMessageDecoder<>(codecConfig.headCodec().get(), codecConfig.bodyCodec().get(), codecConfig.getClientMessageHeadSize(), codecConfig.isDecoderKeepMessageBuf()));
                        ch.pipeline().addLast(new PrivateChannelHandler());
                    }
                });

        config.getChannelOptions().forEach(b::childOption);

        ChannelFuture sync = b.bind(port).sync();
        listenerChannel = sync.channel();

        delegate.onAcceptorStarted();

        if (logger.isInfoEnabled()) {
            logger.info("acceptor listening on port {}", port);
        }
    }


    public Channel getListenerChannel() {
        return listenerChannel;
    }

    public Collection<ProxyAcceptedChannel> getAcceptedChannels() {
        return channels.values();
    }

    public final void shutDown() {
        for (ProxyAcceptedChannel channel : channels.values()) {
            channel.close();
        }
        bossEventLoopGroup.shutdownGracefully();
        workerEventLoopGroup.shutdownGracefully();
    }

    void closeDuplicateChannel(ProxyAcceptedChannel channel) {
        if (channel.getChannelIdentity() == null) {
            return;
        }
        Iterator<ProxyAcceptedChannel> iterator = channels.values().iterator();
        while (iterator.hasNext()) {
            ProxyAcceptedChannel next = iterator.next();
            if (next != channel && Objects.equals(channel.getChannelIdentity(), next.getChannelIdentity())) {
                next.close();
                iterator.remove();
            }
        }
    }

    private synchronized boolean addNewConnectedChannel(ProxyAcceptedChannel newChannel) {
        ChannelId channelId = newChannel.getChannel().id();
        ProxyAcceptedChannel existChannel = channels.get(channelId);
        if (existChannel != null) {
            if (existChannel == newChannel) {
                if (logger.isWarnEnabled()) {
                    logger.warn("channel already exist:{}", newChannel.getChannel());
                }
                return false;
            } else {
                //this is impossible if ChannelId is unique
                if (logger.isWarnEnabled()) {
                    logger.warn("duplicate channel {}， close old one:{}", newChannel.getChannel(), existChannel.getChannel());
                }
                existChannel.close();
            }
        }
        channels.put(channelId, newChannel);
        return true;
    }

    private synchronized boolean removeChannel(ProxyAcceptedChannel oldChannel) {
        ChannelId channelId = oldChannel.getChannel().id();
        ProxyAcceptedChannel existChannel = channels.get(channelId);
        if (existChannel == oldChannel) {
            channels.remove(channelId);
            return true;
        }
        return false;
    }


    private class PrivateChannelHandler extends SimpleChannelInboundHandler<ProxyMessage<?, ?>> {

        private ProxyAcceptedChannel serverChannel;

        //is channel added to acceptor
        private boolean registered;

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            Channel channel = ctx.channel();
            InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();

            if (logger.isInfoEnabled()) {
                logger.info("backendChannel {} active ", address);
            }
            serverChannel = new ProxyAcceptedChannel(ProxyAcceptor.this, channel, config);
            registered = addNewConnectedChannel(serverChannel);
            if (registered) {
                delegate.onChannelStatusEvent(serverChannel, ChannelEvent.CHANNEL_CONNECTED);
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn("register channel:{} fail, close it ", ctx.channel());
                }
                ctx.close();
            }
        }

        //we don't need to release msg here, as ProxyMessage extends ReferenceCounted
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) {
            delegate.onChannelMessage(serverChannel, msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (logger.isInfoEnabled()) {
                logger.info("backendChannel inactive {}", ctx.channel().remoteAddress());
            }
            ctx.close();
            if (registered && removeChannel(serverChannel)) {
                registered = false;
                delegate.onChannelStatusEvent(serverChannel, ChannelEvent.CHANNEL_INACTIVE);
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn("a wild channel:{} inactive, ignore this event", ctx.channel());
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (logger.isInfoEnabled()) {
                logger.info("backendChannel exceptionCaught {} ", ctx.channel().remoteAddress(), cause);
            }
            ctx.close();
            if (registered) {
                delegate.onChannelExceptionCaught(serverChannel, cause);
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn("a wild channel:{} inactive, ignore this event", ctx.channel());
                }
            }
        }
    }
}
