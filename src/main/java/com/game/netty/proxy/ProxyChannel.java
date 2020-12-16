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

package com.game.netty.proxy;

import com.game.netty.GameChannel;
import com.game.netty.config.AbstractChannelConfig;
import io.netty.channel.ChannelFuture;

/**
 * a proxy related channel between a proxy and a logic server
 */
public abstract class ProxyChannel extends GameChannel {

    public ProxyChannel(AbstractChannelConfig config) {
        super(config);
    }

    public final ChannelFuture write(ProxyMessage<?, ?> message) {
        if (channel != null) {
            return channel.write(message);
        }
        return null;
    }

    public final ChannelFuture writeAndFlush(ProxyMessage<?, ?> message) {
        if (channel != null) {
            return channel.writeAndFlush(message);
        }
        return null;
    }
}
