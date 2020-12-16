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

import com.game.netty.client.ClientChannel;
import com.game.netty.client.acceptor.session.SessionInterface;
import com.game.netty.config.AbstractChannelConfig;
import io.netty.channel.Channel;

/**
 * a channel from client to proxy, created by client acceptor
 */
public class ClientAcceptedChannel extends ClientChannel {

    private SessionInterface session;

    public ClientAcceptedChannel(Channel channel, AbstractChannelConfig config) {
        super(config);
        onConnected(channel);
    }

    public SessionInterface getSession() {
        return session;
    }

    void setSession(SessionInterface sessionInterface) {
        this.session = sessionInterface;
    }

    @Override
    public String toString() {
        return "{" +
                "session=" + session +
                ", channel=" + (channel == null ? "null" : channel.remoteAddress().toString()) +
                '}';
    }
}
