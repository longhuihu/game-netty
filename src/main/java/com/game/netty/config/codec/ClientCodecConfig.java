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
import com.game.netty.client.codec.ClientMessageDecoder;
import com.game.netty.client.codec.ClientMessageEncoder;
import com.game.netty.client.codec.MessageBodyCodec;
import com.game.netty.transform.BodyTransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Client Channel Codec related config
 */
public class ClientCodecConfig {

    /**
     * @see ClientMessage
     */
    private final int clientMessageHeadSize;

    /**
     * @see ClientMessageDecoder
     */
    private boolean decoderKeepMessageBuf = false;

    /**
     * @see ClientMessageEncoder
     */
    private final List<BodyTransformer> bodyEncodeTransformers = new ArrayList<>();

    /**
     * @see ClientMessageDecoder
     */
    private final List<BodyTransformer> bodyDecodeTransformers = new ArrayList<>();

    /**
     * @see ClientMessageDecoder
     */
    private final Supplier<MessageBodyCodec<?>> supplier;

    /**
     * CodecSupplier may be stateful, so the parameter is a factory
     *
     * @param clientMessageHeadSize client message head size, must be 0,1,2,4,8
     * @param codecSupplier codecSupplier factory
     */
    public ClientCodecConfig(int clientMessageHeadSize, Supplier<MessageBodyCodec<?>> codecSupplier) {
        this.supplier = codecSupplier;
        this.clientMessageHeadSize = clientMessageHeadSize;
    }

    /**
     * If MessageBodyCodec is stateless, a single instance is enough
     * @param clientMessageHeadSize client message head size, must be 0,1,2,4,8
     * @param codec codec instance
     */
    public ClientCodecConfig(int clientMessageHeadSize, MessageBodyCodec<?> codec) {
        this(clientMessageHeadSize, () -> codec);
    }

    public Supplier<MessageBodyCodec<?>> getSupplier() {
        return supplier;
    }

    public List<BodyTransformer> getBodyEncodeTransformers() {
        return bodyEncodeTransformers;
    }

    public List<BodyTransformer> getBodyDecodeTransformers() {
        return bodyDecodeTransformers;
    }

    /**
     * add body byte buf transformer during message encode
     * pay attention to the order
     * @param transformers one or more body transformers
     */
    public void addBodyEncodeTransformer(BodyTransformer ...transformers) {
        this.bodyEncodeTransformers.addAll(Arrays.asList(transformers));
    }

    /**
     * add body byte buf transformer during message decode
     * pay attention to the order, normally the revers of encode transformers
     * @param transformers one or more body transformers
     */
    public void addBodyDecodeTransformer(BodyTransformer ...transformers) {
        this.bodyDecodeTransformers.addAll(Arrays.asList(transformers));
    }

    public int getClientMessageHeadSize() {
        return clientMessageHeadSize;
    }

    public boolean isDecoderKeepMessageBuf() {
        return decoderKeepMessageBuf;
    }

    public void setDecoderKeepMessageBuf(boolean decoderKeepMessageBuf) {
        this.decoderKeepMessageBuf = decoderKeepMessageBuf;
    }
}
