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
import com.game.netty.proxy.ProxyMessage;

/**
 * delegate for @See ProxyConnector event
 *
 * @param <H> proxy message header type
 * @param <B> message body type
 */
public interface ProxyConnectorDelegate<H, B> {

    /**
     * this to say, connector prepare to establish channel, not mean channel established
     */
    default void onConnectorStart() {
    }

    /**
     * channel status event call back; event like CHANNEL_CONNECTED, CHANNEL_INACTIVE, CHANNEL_IDLE  will be fired on channel eventLoop, other may not
     */
    void onChannelStatusEvent(ProxyConnectChannel channel, ChannelEvent event);

    /**
     * channel exception call back, this will be fired on channel eventLoop
     */
    void onChannelExceptionCaught(ProxyConnectChannel channel, Throwable e);

    /**
     * channel message call back, this will be fired on channel eventLoop
     */
    void onChannelMessage(ProxyConnectChannel channel, ProxyMessage<H, B> message);
}
