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

import com.game.netty.ChannelEvent;
import com.game.netty.client.ClientMessage;
import com.game.netty.client.websocket.ClientWebSocketAcceptor;
import com.game.netty.config.ClientChannelConfig;
import com.game.netty.netty.GameNettyEnv;
import com.game.netty.util.ChannelUtil;
import com.game.netty.client.acceptor.session.SessionInterface;
import com.game.netty.client.acceptor.session.SessionManagerInterface;
import com.game.netty.config.codec.ClientCodecConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * base acceptor to listen client connection
 *
 * @see ClientSocketAcceptor
 * @see ClientWebSocketAcceptor
 */
public abstract class AbstractClientAcceptor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractClientAcceptor.class);

    protected final int port;
    protected final ClientAcceptorDelegate<?> delegate;
    protected final ChannelGroup channelGroup;

    protected EventLoopGroup bossEventLoopGroup;
    protected EventLoopGroup workerEventLoopGroup;

    protected SessionManagerInterface<?> sessionManager;
    protected ClientChannelConfig config;
    protected ClientCodecConfig codecConfig;

    private static final String WRAPPER_CHANNEL_KEY = "com.game.net.wrapper";

    /**
     * @param port     listen port
     * @param delegate delegate to handle acceptor event
     */
    public AbstractClientAcceptor(int port, ClientAcceptorDelegate<?> delegate) {
        this.delegate = delegate;
        this.port = port;
        bossEventLoopGroup = GameNettyEnv.DEFAULT.createBossEventLoopGroup();
        workerEventLoopGroup = GameNettyEnv.DEFAULT.createWorkerEventLoopGroup(0);
        channelGroup = new DefaultChannelGroup(bossEventLoopGroup.next());
    }

    public AbstractClientAcceptor initCodec(ClientCodecConfig codecConfig) {
        this.codecConfig = codecConfig;
        return this;
    }

    public AbstractClientAcceptor initChannel(ClientChannelConfig channelConfig) {
        this.config = channelConfig;
        return this;
    }

    public AbstractClientAcceptor initSessionManager(SessionManagerInterface<?> sessionManager) {
        this.sessionManager = sessionManager;
        return this;
    }

    /**
     * @return 监听Channel
     * @throws InterruptedException interrupt when start listen
     */
    public abstract Channel start() throws InterruptedException;

    public SessionManagerInterface getSessionManager() {
        return sessionManager;
    }

    /**
     * broadcast to all accepted channel
     *
     * @param message must be ByteBuf or ClientMessage
     * @param matcher filter channel to write or not write the message
     */
    public void broadCastMessage(Object message, Predicate<ClientAcceptedChannel> matcher) {
        if (message instanceof ByteBuf || message instanceof ClientMessage) {
            channelGroup.writeAndFlush(message, nettyChannel -> {
                ClientAcceptedChannel channel = ChannelUtil.getAttribute(nettyChannel, WRAPPER_CHANNEL_KEY);
                return matcher.test(channel);
            }, true);
        } else {
            throw new IllegalArgumentException("message must be ByteBuf or ClientMessage");
        }
    }

    private void onChannelAccepted(Channel channel) {
        if (logger.isInfoEnabled()) {
            logger.info("on channel accepted {}", channel.remoteAddress());
        }
        ClientAcceptedChannel clientAcceptedChannel = new ClientAcceptedChannel(channel, config);
        ChannelUtil.setAttribute(channel, WRAPPER_CHANNEL_KEY, clientAcceptedChannel);

        if (sessionManager != null) {
            SessionInterface session = sessionManager.createSessionForNewChannel(clientAcceptedChannel);
            clientAcceptedChannel.setSession(session);
            sessionManager.saveSession(session);
        }

        channelGroup.add(channel);
        try {
            delegate.onChannelStatusEvent(clientAcceptedChannel, ChannelEvent.CHANNEL_CONNECTED);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("delegate handle message error", e);
            }
        }
    }

    private void removeClientChannel(ClientAcceptedChannel channel) {
        if (sessionManager != null) {
            sessionManager.removeSession(channel.getSession().sessionId());
        }
        channelGroup.remove(channel.getChannel());
    }

    public void shutDown() {
        channelGroup.close();
        bossEventLoopGroup.shutdownGracefully();
        workerEventLoopGroup.shutdownGracefully();
    }

    /**
     * 客户端连接的最后一个channel handler，负责转发解码后的消息，及相关网络事件
     */
    @ChannelHandler.Sharable
    public final class ClientChannelHandler extends SimpleChannelInboundHandler<ClientMessage<?>> {

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) {
            onChannelAccepted(ctx.channel());
        }

        //we don't need to release msg here, as ClientMessage extends ReferenceCounted
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ClientMessage msg) {
            ClientAcceptedChannel clientAcceptedChannel = ChannelUtil.getAttribute(ctx.channel(), WRAPPER_CHANNEL_KEY);
            delegate.onChannelMessage(clientAcceptedChannel, msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (logger.isInfoEnabled()) {
                logger.info("gate channel Inactive {}", ctx.channel().remoteAddress());
            }
            ClientAcceptedChannel clientAcceptedChannel = ChannelUtil.getAttribute(ctx.channel(), WRAPPER_CHANNEL_KEY);
            removeClientChannel(clientAcceptedChannel);
            ctx.close();
            delegate.onChannelStatusEvent(clientAcceptedChannel, ChannelEvent.CHANNEL_INACTIVE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof IOException || cause instanceof TimeoutException) {
                if (logger.isInfoEnabled()) {
                    logger.info("channel:{} exceptionCaught:{}", ctx.channel().remoteAddress(), cause);
                }
            } else {
                if (logger.isErrorEnabled()) {
                    logger.error("channel:{} exceptionCaught:{}", ctx.channel().remoteAddress(), cause);
                }
            }
            ClientAcceptedChannel clientAcceptedChannel = ChannelUtil.getAttribute(ctx.channel(), WRAPPER_CHANNEL_KEY);
            ctx.close();
            delegate.onChannelExceptionCaught(clientAcceptedChannel, cause);
        }
    }
}
