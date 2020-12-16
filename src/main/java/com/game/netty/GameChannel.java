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
package com.game.netty;

import com.game.netty.util.ChannelUtil;
import com.game.netty.config.AbstractChannelConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOutboundBuffer;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * GameChannel is a wrapper for Netty Channel
 */
public abstract class GameChannel {

    protected Channel channel;

    private ScheduledFuture<?> flushFuture;

    private final AbstractChannelConfig config;

    public GameChannel(AbstractChannelConfig config) {
        this.config = config;
    }

    public void onConnected(Channel channel) {
        this.channel = channel;
        if (config.getAutoFlushIntervalMillis() > 0) {
            setAutoFlush(config.getAutoFlushIntervalMillis());
        }
    }

    public final void setAutoFlush(int intervalMillis) {
        if (flushFuture != null) {
            throw new IllegalStateException("auto flush already set");
        }
        if (intervalMillis > 0) {
            flushFuture = channel.eventLoop().scheduleAtFixedRate(this::flushTask, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    public final Channel getChannel() {
        return channel;
    }

    public final void close() {
        if (channel != null) {
            channel.close();
            if (flushFuture != null) {
                flushFuture.cancel(true);
                flushFuture = null;
            }
        }
    }

    /**
     * 越过编码器发送字节数据到底层
     *
     * @param byteBuf
     */
    public final ChannelFuture writeBuf(ByteBuf byteBuf) {
        if (channel != null) {
            return channel.write(byteBuf);
        }
        return null;
    }

    public final ChannelFuture writeAndFlush(ByteBuf byteBuf) {
        if (channel != null) {
            return channel.writeAndFlush(byteBuf);
        }
        return null;
    }

    public final void flush() {
        if (channel != null) {
            channel.flush();
        }
    }

    private void flushTask() {
        ChannelOutboundBuffer outboundBuffer = channel.unsafe().outboundBuffer();

        //channel可能已经关闭
        if (outboundBuffer == null) {
            return;
        }
        long bytes = outboundBuffer.totalPendingWriteBytes();
        if (bytes <= 0) return;

        channel.flush();
    }

    public void setAttribute(String key, Object value) {
        ChannelUtil.setAttribute(channel, key, value);
    }

    public <T> T getAttribute(String key) {
        return ChannelUtil.getAttribute(channel, key);
    }
}
