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

package com.game.netty.client.acceptor.session;

import com.game.netty.client.acceptor.ClientAcceptedChannel;

/**
 * Base Session to help implementation
 */
public abstract class AbstractSession implements SessionInterface {

    private final String sessionId;
    private final ClientAcceptedChannel channel;

    public AbstractSession(String sessionId, ClientAcceptedChannel channel) {
        this.sessionId = sessionId;
        this.channel = channel;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public ClientAcceptedChannel channel() {
        return channel;
    }

    @Override
    public String toString() {
        return "{" +
                "sessionId='" + sessionId + '\'' +
                '}';
    }
}
