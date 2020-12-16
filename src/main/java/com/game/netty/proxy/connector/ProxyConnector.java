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

package com.game.netty.proxy.connector;

import com.game.netty.ChannelEvent;
import com.game.netty.ServerDefine;
import com.game.netty.proxy.ProxyMessage;
import com.game.netty.netty.GameNettyEnv;
import com.game.netty.netty.GameNettyUtil;
import com.game.netty.proxy.codec.ProxyMessageDecoder;
import com.game.netty.proxy.codec.ProxyMessageEncoder;
import com.game.netty.config.ProxyChannelConfig;
import com.game.netty.config.codec.ProxyCodecConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * connector for proxy to initiate connect to logic server
 */
@SuppressWarnings("unused")
public class ProxyConnector {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(ProxyConnector.class);

    private final Map<Integer, ProxyConnectChannel> channelMap = new ConcurrentHashMap<>();

    private ProxyChannelConfig config;
    private ProxyCodecConfig codecConfig;

    private final List<ServerDefine> servers = new CopyOnWriteArrayList<>();

    private final EventLoopGroup workerGroup;

    private ProxyMessageEncoder<?, ?> sharedEncoder;

    private final ProxyConnectorDelegate<?, ?> delegate;

    private final ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> checkChannelTaskFuture;

    private final AtomicBoolean started = new AtomicBoolean(false);

    public ProxyConnector(ProxyConnectorDelegate<?, ?> delegate) {
        this.workerGroup = GameNettyEnv.DEFAULT.createWorkerEventLoopGroup(0);
        this.delegate = delegate;
        scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    }

    public ProxyConnector initCodec(ProxyCodecConfig codecConfig) {
        this.codecConfig = codecConfig;
        return this;
    }

    public ProxyConnector initChannel(ProxyChannelConfig config) {
        this.config = config;
        return this;
    }

    public void start(List<ServerDefine> servers) {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("connector has already started");
        }
        updateRemoteServers(servers);
        sharedEncoder = new ProxyMessageEncoder<>(codecConfig.headCodec().get(), codecConfig.bodyCodec().get(), codecConfig.getClientMessageHeadSize());
        scheduledExecutorService.execute(() -> {
            checkChannels();
            this.delegate.onConnectorStart();
        });
        checkChannelTaskFuture = scheduledExecutorService.scheduleAtFixedRate(this::checkChannels, 5, 5, TimeUnit.SECONDS);
    }

    public void updateRemoteServers(List<ServerDefine> servers) {
        this.servers.clear();
        this.servers.addAll(servers);
    }

    public void shutDown() {
        if (started.compareAndSet(true, false)) {
            for (Map.Entry<Integer, ProxyConnectChannel> e : channelMap.entrySet()) {
                e.getValue().close();
            }
            checkChannelTaskFuture.cancel(true);
            checkChannelTaskFuture = null;
        }
    }


    public ProxyConnectChannel getChannel(int serverId) {
        return channelMap.get(serverId);
    }

    private void checkChannels() {
        if (!started.get()) {
            return;
        }
        try {
            doCheckChannels();
        } catch (Exception e) {
            log.error("checkChannels", e);
        }
    }

    private void doCheckChannels() {
        // 关闭过期链接
        Iterator<Map.Entry<Integer, ProxyConnectChannel>> iterator = channelMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ProxyConnectChannel> entry = iterator.next();

            boolean find = false;
            for (ServerDefine server : servers) {
                if (server.equals(entry.getValue().getRemoteServer())) {
                    find = true;
                    break;
                }
            }
            if (!find) {
                entry.getValue().close();
                iterator.remove();
                log.info("remove obsolete channel {}", entry.getValue());
            }
        }

        //为新加入的server建立连接
        for (ServerDefine server : servers) {
            final ProxyConnectChannel serverChannel = channelMap.get(server.getServerId());
            if (serverChannel != null) {
                if (!serverChannel.isConnected() && serverChannel.tryLockConnectingStatus()) {
                    scheduledExecutorService.execute(() -> connect(serverChannel));
                }
            } else {
                ProxyConnectChannel newChannel = new ProxyConnectChannel(server, config);
                if (channelMap.putIfAbsent(server.getServerId(), newChannel) == null) {
                    if (newChannel.tryLockConnectingStatus()) {
                        scheduledExecutorService.execute(() -> connect(newChannel));
                    }
                }
            }
        }
    }

    private void connect(ProxyConnectChannel serverChannel) {
        ServerDefine server = serverChannel.getRemoteServer();
        try {
            delegate.onChannelStatusEvent(serverChannel, ChannelEvent.CHANNEL_CONNECT);

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
                    ch.pipeline().addLast(new ProxyMessageDecoder<>(codecConfig.headCodec().get(), codecConfig.bodyCodec().get(), codecConfig.getClientMessageHeadSize(), codecConfig.isDecoderKeepMessageBuf()));
                    ch.pipeline().addLast(new PrivateHandler(serverChannel));
                }
            });

            config.getChannelOptions().forEach(b::option);
            SocketAddress address = new InetSocketAddress(server.getIp(), server.getPort());
            ChannelFuture f = b.connect(address).sync();
            if (f.isSuccess()) {
                log.info("connected to {}", server);
            } else {
                log.warn("connect fail to {}", server);
                delegate.onChannelStatusEvent(serverChannel, ChannelEvent.CHANNEL_CONNECT_FAIL);
            }
        } catch (Exception e) {
            log.warn("connect fail to {} {}", server, e);
        } finally {
            serverChannel.unLockConnectingStatus();
        }
    }

    public final class PrivateHandler extends SimpleChannelInboundHandler<ProxyMessage<?, ?>> {

        private final ProxyConnectChannel serverChannel;

        public PrivateHandler(ProxyConnectChannel serverChannel) {
            this.serverChannel = serverChannel;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("backendChannel active {}", serverChannel);
            serverChannel.onConnected(ctx.channel());
            delegate.onChannelStatusEvent(serverChannel, ChannelEvent.CHANNEL_CONNECTED);
        }

        //we don't need to release msg here, as ProxyMessage extends ReferenceCounted
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) {
            delegate.onChannelMessage(serverChannel, msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("backendChannel inactive {}", serverChannel);
            serverChannel.close();
            delegate.onChannelStatusEvent(serverChannel, ChannelEvent.CHANNEL_INACTIVE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof IOException) {
                log.info("backendChannel:{} exceptionCaught:{}", serverChannel, cause);
            } else {
                log.error("backendChannel:{} exceptionCaught:{}", serverChannel, cause);
            }
            ctx.close();
            delegate.onChannelExceptionCaught(serverChannel, cause);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
                IdleStateEvent event = (IdleStateEvent) evt;
                delegate.onChannelStatusEvent(serverChannel, ChannelEvent.CHANNEL_IDLE);
            }
        }
    }
}
