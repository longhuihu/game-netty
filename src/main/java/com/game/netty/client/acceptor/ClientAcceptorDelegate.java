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

import com.game.netty.ChannelEvent;
import com.game.netty.client.ClientMessage;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 * delegate to handle client Acceptor events
 *
 * @param <B> message body type
 */
public interface ClientAcceptorDelegate<B> {

    /**
     * only called by ClientWebSocketAcceptor.
     * return valid SSLContext to use WSS, or null to use WS
     */
    default SSLContext createSSLContext() {
        return null;
    }

    /**
     * called on acceptor start to listen on port.
     * this callback will be triggered async after ClientSocketAcceptor.start() or ClientWebSocketAcceptor.start()
     */
    default void onAcceptorStarted() {
    }

    /**
     * channel status event call back;
     * event like CHANNEL_CONNECTED, CHANNEL_INACTIVE, CHANNEL_IDLE  will be fired in channel eventLoop, other may not
     */
    void onChannelStatusEvent(ClientAcceptedChannel channel, ChannelEvent event);

    /**
     * channel exception call back, this will be fired in channel eventLoop
     */
    void onChannelExceptionCaught(ClientAcceptedChannel channel, Throwable e);

    /**
     * channel message call back, this will be fired in channel eventLoop
     */
    void onChannelMessage(ClientAcceptedChannel channel, ClientMessage<B> message);
}
