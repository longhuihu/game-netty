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

package com.game.netty.client.connector;

import com.game.netty.ChannelEvent;
import com.game.netty.ServerDefine;
import com.game.netty.client.codec.ClientMessageDecoder;
import com.game.netty.client.codec.ClientMessageEncoder;
import com.game.netty.client.ClientMessage;
import com.game.netty.netty.GameNettyEnv;
import com.game.netty.netty.GameNettyUtil;
import com.game.netty.config.ClientChannelConfig;
import com.game.netty.config.codec.ClientCodecConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used by Client, connect to proxy server
 */
public class ClientConnector {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(ClientConnector.class);

    private final Map<Integer, ClientConnectChannel> channelMap = new ConcurrentHashMap<>();

    private ClientChannelConfig config = new ClientChannelConfig();
    private ClientCodecConfig codecConfig;
    private ClientMessageEncoder<?> sharedEncoder;

    private final List<ServerDefine> servers = new CopyOnWriteArrayList<>();

    private final EventLoopGroup workerGroup;

    private final ClientConnectorDelegate<?> delegate;

    private final ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> checkChannelTaskFuture;

    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * @param delegate to handle connector events
     */
    public ClientConnector(ClientConnectorDelegate<?> delegate) {
        this.workerGroup = GameNettyEnv.DEFAULT.createWorkerEventLoopGroup(0);
        this.delegate = delegate;
        scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    }

    /**
     * @param codecConfig channel codec config
     */
    public ClientConnector initCodec(ClientCodecConfig codecConfig) {
        this.codecConfig = codecConfig;
        return this;
    }

    /**
     * @param config channel option config
     */
    public ClientConnector initChannel(ClientChannelConfig config) {
        this.config = config;
        return this;
    }

    /**
     * @param servers connection target servers
     */
    public void start(List<ServerDefine> servers) {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("connector has already started");
        }
        setServers(servers);
        sharedEncoder = new ClientMessageEncoder<>(codecConfig.getSupplier().get(), codecConfig.getClientMessageHeadSize());
        codecConfig.getBodyEncodeTransformers().forEach(sharedEncoder::addTransformer);

        scheduledExecutorService.execute(() -> {
            checkChannels();
            this.delegate.onConnectorStart();
        });
        checkChannelTaskFuture = scheduledExecutorService.scheduleAtFixedRate(this::checkChannels, 5, 5, TimeUnit.SECONDS);
    }


    public void setServers(List<ServerDefine> servers) {
        this.servers.clear();
        this.servers.addAll(servers);
    }

    public void shutDown() {
        if (started.compareAndSet(true, false)) {
            for (Map.Entry<Integer, ClientConnectChannel> e : channelMap.entrySet()) {
                e.getValue().close();
            }
            checkChannelTaskFuture.cancel(true);
            checkChannelTaskFuture = null;
        }
    }

    public ClientConnectChannel getChannel(int serverId) {
        return channelMap.get(serverId);
    }

    private void checkChannels() {
        if (!started.get()) {
            return;
        }

        //close obsolete channels
        List<ClientConnectChannel> channelToClose = new ArrayList<>();
        for (Map.Entry<Integer, ClientConnectChannel> e : channelMap.entrySet()) {
            ClientConnectChannel channel = e.getValue();
            boolean find = false;
            for (ServerDefine server : servers) {
                if (server.equals(channel.getServer())) {
                    find = true;
                    break;
                }
            }
            if (!find) {
                channelToClose.add(channel);
            }
        }
        for (ClientConnectChannel channel : channelToClose) {
            channel.close();
            channelMap.remove(channel.getServer().getServerId());
            log.info("delete client channel {}", channel.getServer());
        }

        //create channel for new servers
        for (ServerDefine server : servers) {
            final ClientConnectChannel clientChannel = channelMap.get(server.getServerId());
            if (clientChannel != null) {
                if (!clientChannel.isConnected() && clientChannel.tryLockConnectingStatus()) {
                    scheduledExecutorService.execute(() -> connect(clientChannel));
                }
            } else {
                ClientConnectChannel newChannel = new ClientConnectChannel(server, config);
                if (channelMap.putIfAbsent(server.getServerId(), newChannel) == null) {
                    if (newChannel.tryLockConnectingStatus()) {
                        scheduledExecutorService.execute(() -> connect(newChannel));
                    }
                }
            }
        }
    }

    private void connect(ClientConnectChannel clientChannel) {
        ServerDefine server = clientChannel.getServer();
        try {
            delegate.onChannelStatusEvent(clientChannel, ChannelEvent.CHANNEL_CONNECT);
            Bootstrap b = new Bootstrap();
            b.group(workerGroup).channel(GameNettyEnv.DEFAULT.clientChannelClass()).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    int idleTime = config.getChannelIdleSecond();
                    if (idleTime > 0) {
                        ch.pipeline().addLast(new IdleStateHandler(idleTime, 0, 0));
                    }
                    if (config.getChannelReadTimeOut() > 0) {
                        ch.pipeline().addLast(new ReadTimeoutHandler(config.getChannelReadTimeOut()));
                    }
                    if (GameNettyUtil.getLogLevel() != null) {
                        ch.pipeline().addLast(new LoggingHandler(GameNettyUtil.getLogLevel()));
                    }
                    ch.pipeline().addLast(sharedEncoder);
                    ch.pipeline().addLast(new ClientMessageDecoder<>(codecConfig.getSupplier().get(), codecConfig.getClientMessageHeadSize(), codecConfig.isDecoderKeepMessageBuf()));
                    ch.pipeline().addLast(new PrivateHandler(clientChannel));
                }
            });

            config.getChannelOptions().forEach(b::option);

            SocketAddress address = new InetSocketAddress(server.getIp(), server.getPort());
            ChannelFuture f = b.connect(address).sync();
            if (f.isSuccess()) {
                log.info("connected to {}", server);
            } else {
                log.warn("connect fail to {}", server);
                delegate.onChannelStatusEvent(clientChannel, ChannelEvent.CHANNEL_CONNECT_FAIL);
            }
        } catch (Exception e) {
            log.warn("connect fail to {} {}", server, e);
            delegate.onChannelStatusEvent(clientChannel, ChannelEvent.CHANNEL_CONNECT_FAIL);
        } finally {
            clientChannel.unLockConnectingStatus();
        }
    }

    public final class PrivateHandler extends SimpleChannelInboundHandler<ClientMessage<?>> {

        private final ClientConnectChannel clientChannel;

        public PrivateHandler(ClientConnectChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("backendChannel active {}", clientChannel);
            clientChannel.onConnected(ctx.channel());
            delegate.onChannelStatusEvent(clientChannel, ChannelEvent.CHANNEL_CONNECTED);
        }

        //we don't need to release msg here, as ClientMessage extends ReferenceCounted
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ClientMessage msg) {
            delegate.onChannelMessage(clientChannel, msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("backendChannel inactive {}", clientChannel);
            clientChannel.close();
            delegate.onChannelStatusEvent(clientChannel, ChannelEvent.CHANNEL_INACTIVE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("backendChannel:{} exceptionCaught:{}", clientChannel, cause);
            ctx.close();
            delegate.onChannelExceptionCaught(clientChannel, cause);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
                delegate.onChannelStatusEvent(clientChannel, ChannelEvent.CHANNEL_IDLE);
            }
        }
    }
}
