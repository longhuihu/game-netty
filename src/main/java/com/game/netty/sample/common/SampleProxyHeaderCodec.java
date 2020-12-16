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

package com.game.netty.sample.common;

import com.game.netty.proxy.codec.ProxyHeaderCodec;
import io.netty.buffer.ByteBuf;

public class SampleProxyHeaderCodec implements ProxyHeaderCodec<String> {

    @Override
    public int proxyHeadEncodeSize(String header) {
        return header.getBytes().length;
    }

    @Override
    public void encodeProxyHead(String header, ByteBuf out) {
        out.writeBytes(header.getBytes());
    }

    @Override
    public String decodeProxyHead(ByteBuf in, int length) {
        byte[] dst = new byte[length];
        in.readBytes(dst);
        return new String(dst);
    }
}
