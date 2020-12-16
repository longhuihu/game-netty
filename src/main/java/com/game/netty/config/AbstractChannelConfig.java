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

package com.game.netty.config;

import io.netty.channel.ChannelOption;

import java.util.HashMap;
import java.util.Map;


/**
 * channel config collect Netty Channel Options and other parameters
 */
public abstract class AbstractChannelConfig {

    private final Map<ChannelOption, Object> channelOptions = new HashMap<>();

    /**
     * socket channel read timeout, if channel read no data until timeout, will be closed
     * channelReadTimeOut = 0 to disable
     */
    private int channelReadTimeOut = 0;

    /**
     * channel idle timeout, if channel read no data until timeout, ChannelEvent.CHANNEL_IDLE will be fired
     * channelIdleSecond = 0 to disable
     */
    private int channelIdleSecond = 0;

    /**
     * auto flush interval, if set 0，disable it, then user must call GameChannel.writeAndFlush or GameChannel.flush
     */
    private int autoFlushIntervalMillis = 0;


    public int getChannelReadTimeOut() {
        return channelReadTimeOut;
    }

    public void setChannelReadTimeOut(int channelReadTimeOut) {
        this.channelReadTimeOut = channelReadTimeOut;
    }

    public void setChannelIdleSecond(int channelIdleSecond) {
        this.channelIdleSecond = channelIdleSecond;
    }

    public int getChannelIdleSecond() {
        return channelIdleSecond;
    }

    public int getAutoFlushIntervalMillis() {
        return autoFlushIntervalMillis;
    }

    public void setAutoFlushIntervalMillis(int autoFlushIntervalMillis) {
        this.autoFlushIntervalMillis = autoFlushIntervalMillis;
    }

    public void addChanelOption(ChannelOption option, Object value) {
        channelOptions.put(option, value);
    }

    public Map<ChannelOption, Object> getChannelOptions() {
        return channelOptions;
    }

}
