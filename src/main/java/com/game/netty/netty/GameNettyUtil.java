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

package com.game.netty.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;

/**
 * Help to config Netty, keep you from lower netty api
 */
public class GameNettyUtil {

    static final GameNettyEnv ENV;

    /**
     * Config Netty ByteBuf leak detection，this may break with future netty version
     *
     * The property names in the function is defined at ResourceLeakDetector
     *
     * @param level          must be [0,3]，correspond to ResourceLeakDetector.Level定义; value 0 mean disable
     * @param traceRecords   max ByteBuf trace records count，0 for default
     * @param sampleInterval ByteBuf trace interval, 0 for default
     */
    public static void enableByteBufLeakDetect(int level, int traceRecords, int sampleInterval) {
        level = Math.max(0, Math.min(3, level));
        System.setProperty("io.netty.leakDetection.level", level + "");
        if (level > 0) {
            System.setProperty("io.netty.allocator.type", "unpooled");
            if (traceRecords > 0) {
                System.setProperty("io.netty.leakDetection.targetRecords", traceRecords + "");
            }
            if (sampleInterval > 0) {
                System.setProperty("io.netty.leakDetection.samplingInterval", sampleInterval + "");
            }
        }
    }

    /**
     * log level, null means disable
     * Attention: you also need to modify you log system level——like log4j
     */
    private static LogLevel logLevel;


    public static LogLevel getLogLevel() {
        return logLevel;
    }

    public static void setLogLevel(LogLevel logLevel) {
        GameNettyUtil.logLevel = logLevel;
    }

    /**
     * create default env
     */
    static {
        if (Epoll.isAvailable()) {
            ENV = new EpollChannelConfigImpl();
        } else if (KQueue.isAvailable()) {
            ENV = new KQueueChannelConfigImpl();
        } else {
            ENV = new NioChannelConfigImpl();
        }
    }

    private GameNettyUtil() {

    }

    /**
     * GameNettyEnv using epoll
     */
    static class EpollChannelConfigImpl implements GameNettyEnv {

        @Override
        public Class<EpollServerSocketChannel> serverChannelClass() {
            return EpollServerSocketChannel.class;
        }

        @Override
        public Class<EpollSocketChannel> clientChannelClass() {
            return EpollSocketChannel.class;
        }

        @Override
        public EventLoopGroup createBossEventLoopGroup() {
            return new EpollEventLoopGroup(1);
        }

        @Override
        public EventLoopGroup createWorkerEventLoopGroup(int nThreads) {
            return new EpollEventLoopGroup(nThreads);
        }
    }

    /**
     * GameNettyEnv using java nio
     */
    static class NioChannelConfigImpl implements GameNettyEnv {

        @Override
        public Class<NioServerSocketChannel> serverChannelClass() {
            return NioServerSocketChannel.class;
        }

        @Override
        public Class<NioSocketChannel> clientChannelClass() {
            return NioSocketChannel.class;
        }

        @Override
        public EventLoopGroup createBossEventLoopGroup() {
            return new NioEventLoopGroup(1);
        }

        @Override
        public EventLoopGroup createWorkerEventLoopGroup(int nThread) {
            return new NioEventLoopGroup(nThread);
        }
    }

    /**
     * GameNettyEnv using KQueue
     */
    static class KQueueChannelConfigImpl implements GameNettyEnv {

        @Override
        public Class<KQueueServerSocketChannel> serverChannelClass() {
            return KQueueServerSocketChannel.class;
        }

        @Override
        public Class<KQueueSocketChannel> clientChannelClass() {
            return KQueueSocketChannel.class;
        }

        @Override
        public EventLoopGroup createBossEventLoopGroup() {
            return new KQueueEventLoopGroup(1);
        }

        @Override
        public EventLoopGroup createWorkerEventLoopGroup(int nThreads) {
            return new KQueueEventLoopGroup(nThreads);
        }
    }
}
