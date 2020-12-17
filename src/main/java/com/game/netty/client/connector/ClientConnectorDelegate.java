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

import com.game.netty.ChannelEvent;
import com.game.netty.client.ClientMessage;

/**
 * ClientConnectChannel event delegate
 * <p>
 * only ChannelEvent value like CHANNEL_CONNECTED, CHANNEL_INACTIVE, CHANNEL_IDLE
 */
public interface ClientConnectorDelegate<B> {

    /**
     * this to say, connector prepare to establish channel, not mean any channel has been established
     * this will be called async, after ClientConnector.start()
     */
    default void onConnectorStart() {
    }

    /**
     * channel status event call back; event like CHANNEL_CONNECTED, CHANNEL_INACTIVE, CHANNEL_IDLE  will be fired on channel eventLoop, other may not
     * @param channel the channel that fire event
     * @param event the event
     */
    void onChannelStatusEvent(ClientConnectChannel channel, ChannelEvent event);

    /**
     * channel exception call back, this will be fired on channel eventLoop
     * @param channel the channel that fire event
     * @param e the exception happened
     */
    void onChannelExceptionCaught(ClientConnectChannel channel, Throwable e);

    /**
     * channel message call back, this will be fired on channel eventLoop
     * @param channel the channel that fire event
     * @param message the received message
     */
    void onChannelMessage(ClientConnectChannel channel, ClientMessage<B> message);
}
