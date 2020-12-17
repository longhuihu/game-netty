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

package com.game.netty.config.codec;

import com.game.netty.client.ClientMessage;
import com.game.netty.client.codec.MessageBodyCodec;
import com.game.netty.proxy.codec.ProxyHeaderCodec;
import com.game.netty.proxy.codec.ProxyMessageDecoder;

import java.util.function.Supplier;

/**
 * Proxy Channel Codec related config
 */
public class ProxyCodecConfig {

    /**
     * @see ClientMessage
     */
    private final int clientMessageHeadSize;

    /**
     * @see ProxyHeaderCodec
     */
    private final Supplier<ProxyHeaderCodec<?>> headCodecSupplier;

    /**
     * @see MessageBodyCodec
     */
    private final Supplier<MessageBodyCodec<?>> bodyCodecSupplier;

    /**
     * @see ProxyMessageDecoder
     */
    private boolean decoderKeepMessageBuf;

    /**
     * ProxyHeaderCodec or MessageBodyCodec may be stateful, so the parameter is a factory
     *
     * @param clientMessageHeadSize client message head size, must be 0,1,2,4,8
     * @param headCodecSupplier ProxyHeaderCodec factory
     * @param bodyCodecSupplier MessageBodyCodec factory
     */
    public ProxyCodecConfig(int clientMessageHeadSize, Supplier<ProxyHeaderCodec<?>> headCodecSupplier, Supplier<MessageBodyCodec<?>> bodyCodecSupplier) {
        this.bodyCodecSupplier = bodyCodecSupplier;
        this.headCodecSupplier = headCodecSupplier;
        this.clientMessageHeadSize = clientMessageHeadSize;
    }

    /**
     * If ProxyHeaderCodec or MessageBodyCodec is stateless, a single instance is enough
     *
     * @param clientMessageHeadSize client message head size, must be 0,1,2,4,8
     * @param headerCodec ProxyHeaderCodec instance
     * @param bodyCodec   MessageBodyCodec instance
     */
    public ProxyCodecConfig(int clientMessageHeadSize, ProxyHeaderCodec<?> headerCodec, MessageBodyCodec<?> bodyCodec) {
        this(clientMessageHeadSize, () -> headerCodec, () -> bodyCodec);
    }

    public boolean isDecoderKeepMessageBuf() {
        return decoderKeepMessageBuf;
    }

    public void setDecoderKeepMessageBuf(boolean decoderKeepMessageBuf) {
        this.decoderKeepMessageBuf = decoderKeepMessageBuf;
    }

    public Supplier<ProxyHeaderCodec<?>> headCodec() {
        return headCodecSupplier;
    }

    public Supplier<MessageBodyCodec<?>> bodyCodec() {
        return bodyCodecSupplier;
    }

    public int getClientMessageHeadSize() {
        return clientMessageHeadSize;
    }
}
