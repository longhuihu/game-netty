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

package com.game.netty.client.codec;

import com.game.netty.client.ClientMessage;
import io.netty.buffer.ByteBuf;

/**
 * @param <B> body type
 * @see ClientMessage body codec interface
 */
public interface MessageBodyCodec<B> {

    /**
     * compute encoded message body byte size
     *
     * @param body target message body object
     * @return encoded byte size
     */
    default int bodyEncodeSize(B body) {
        return 0;
    }

    /**
     * encode message body and write into byteBuf.
     * on write complete, the bodyBuf.readIndex should not change, the bodyBuf.writeIndex point to the body end.
     * <p>
     * GameNetty make sure out is a auto extending ByteBuf.
     *
     * @param body target message body
     * @param out  byteBuf to write message
     */
    default void encodeBody(B body, ByteBuf out) {
    }

    /**
     * decode byteBuf into body object：
     * 1. when called，bodyBuf.readIndex is at body position, param length is body byte size;
     * 2. implementation can not change bodyBuf
     *
     * @param messageHead message head
     * @param bodyBuf     byteBuf that contains the message body
     * @param length      the body size
     * @return decoded body object
     */
    default B decodeMessageBody(long messageHead, ByteBuf bodyBuf, int length) {
        return null;
    }

}
