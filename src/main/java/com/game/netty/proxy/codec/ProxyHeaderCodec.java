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

package com.game.netty.proxy.codec;

import io.netty.buffer.ByteBuf;

/**
 * Codec interface for coding @See ProxyHeader
 *
 * @param <H> ProxyHeader type
 */
public interface ProxyHeaderCodec<H> {

    /**
     * 计算proxyHeader的编码长度
     *
     * @param header proxy head
     * @return encoded byte length
     */
    int proxyHeadEncodeSize(H header);

    /**
     * 编码proxyHeader，写入输出缓冲区out
     *
     * @param header proxy head
     * @param out byte buf to write
     */
    void encodeProxyHead(H header, ByteBuf out);

    /**
     * 解码proxyMessage头部，in.readIndex指向head开始位置
     *
     * @param in buf to read
     * @param headLength  byte length of proxy head
     * @return decoded proxy head object
     */
    H decodeProxyHead(ByteBuf in, int headLength);
}
