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

import com.game.netty.ServerDefine;
import com.game.netty.client.ClientChannel;
import com.game.netty.config.AbstractChannelConfig;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A channel from client to proxy
 */
public class ClientConnectChannel extends ClientChannel {

    private final ServerDefine server;
    private final AtomicBoolean isConnecting;

    public ClientConnectChannel(ServerDefine server, AbstractChannelConfig config) {
        super(config);
        this.server = server;
        this.isConnecting = new AtomicBoolean(false);
    }

    public final ServerDefine getServer() {
        return server;
    }

    /**
     * for inner use
     *
     * @return
     */
    boolean tryLockConnectingStatus() {
        return isConnecting.compareAndSet(false, true);
    }

    void unLockConnectingStatus() {
        isConnecting.set(false);
    }

    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    @Override
    public String toString() {
        return "{" +
                "server=" + server +
                ", isConnecting=" + isConnecting +
                ", channel=" + (channel == null ? "null" : channel.remoteAddress().toString()) +
                '}';
    }
}
