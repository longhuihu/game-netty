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

import com.game.netty.client.ClientMessage;
import io.netty.util.ReferenceCounted;

/**
 * A message between proxy and logic server.
 * <p>
 * ProxyMessage is composed by two parts: the raw ClientMessage and a ProxyHead appended by proxy or logic server.
 * <p>
 * Using ProxyHead, Proxy can attach some information about the current client channel, which are probably needed by logic server.
 *
 * @param <H> ProxyHead type
 * @param <B> ClientMessage Body type
 */
public class ProxyMessage<H, B> implements ReferenceCounted {

    private final ClientMessage<B> clientMessage;
    private final H proxyHeader;


    /**
     * @param header header data
     * @param clientMessage client message, Attention: ownership transfer happened here
     */
    public ProxyMessage(H header, ClientMessage<B> clientMessage) {
        this.clientMessage = clientMessage;
        this.proxyHeader = header;
    }

    public ClientMessage<B> clientMessage() {
        return clientMessage;
    }

    public H proxyHeader() {
        return proxyHeader;
    }

    @Override
    public ProxyMessage<H, B> retain() {
        clientMessage.retain();
        return this;
    }

    @Override
    public ProxyMessage<H, B> retain(int increment) {
        clientMessage.retain(increment);
        return this;
    }

    @Override
    public ProxyMessage<H, B> touch() {
        clientMessage.touch();
        return this;
    }

    @Override
    public ProxyMessage<H, B> touch(Object hint) {
        clientMessage.touch(hint);
        return this;
    }

    @Override
    public int refCnt() {
        return clientMessage.refCnt();
    }

    @Override
    public boolean release() {
        return clientMessage.release();
    }

    @Override
    public boolean release(int decrement) {
        return clientMessage.release(decrement);
    }
}
