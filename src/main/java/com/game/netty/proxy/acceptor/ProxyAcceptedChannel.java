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

import com.game.netty.proxy.ProxyChannel;
import com.game.netty.config.AbstractChannelConfig;
import io.netty.channel.Channel;

/**
 * A channel at logic server, presents a channel from a proxy
 */
public class ProxyAcceptedChannel extends ProxyChannel {

    /**
     * the identification of the Channel
     * How to identify a channel is your business, you can even ignore it
     * But, if you set channelIdentity，ServerAcceptor will try to keep uniqueness：close duplicate channel
     */
    private Object channelIdentity;

    private final ProxyAcceptor acceptor;


    public ProxyAcceptedChannel(ProxyAcceptor acceptor, Channel socketChannel, AbstractChannelConfig config) {
        super(config);
        this.acceptor = acceptor;
        onConnected(socketChannel);
    }

    public Object getChannelIdentity() {
        return channelIdentity;
    }

    public void setChannelIdentity(Object channelIdentity) {
        if (this.channelIdentity != null) {
            throw new IllegalStateException("channelIdentity already set for " + channel);
        }
        this.channelIdentity = channelIdentity;
        acceptor.closeDuplicateChannel(this);
    }

    @Override
    public String toString() {
        return "{" +
                "channel=" + channel.remoteAddress() +
                ",channelIdentity=" + channelIdentity +
                '}';
    }
}
