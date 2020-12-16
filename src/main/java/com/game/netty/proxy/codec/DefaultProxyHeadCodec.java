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

import com.game.netty.proxy.DefaultProxyHead;
import com.game.netty.util.StringTool;
import io.netty.buffer.ByteBuf;


/**
 * DefaultProxyHead codec implementation
 */
public class DefaultProxyHeadCodec implements ProxyHeaderCodec<DefaultProxyHead> {

    /**
     * compute the DefaultProxyHead encoded length
     *
     * @param header proxy header object
     * @return length of encode size
     */
    @Override
    public int proxyHeadEncodeSize(DefaultProxyHead header) {
        int length = Long.BYTES; //user id
        length += Byte.BYTES; //session length field
        if (!StringTool.isEmpty(header.getSessionId())) {
            length += header.getSessionId().length(); //session body field
        }
        length += Byte.BYTES; //ip length field
        if (!StringTool.isEmpty(header.getIp())) {
            length += header.getIp().length(); //session body field
        }
        return length;
    }

    /**
     * encode the DefaultProxyHead into out buf
     */
    @Override
    public void encodeProxyHead(DefaultProxyHead header, ByteBuf out) {
        //写用户ID
        out.writeLong(header.getUserId());

        //写sessionId
        if (!StringTool.isEmpty(header.getSessionId())) {
            int sessionLength = header.getSessionId().length();
            if (sessionLength > Byte.MAX_VALUE) {
                throw new IllegalArgumentException("session too long");
            }
            out.writeByte(sessionLength);
            out.writeBytes(header.getSessionId().getBytes());
        } else {
            out.writeByte(0);
        }

        //写ip
        if (!StringTool.isEmpty(header.getIp())) {
            int ipLength = header.getIp().length();
            if (ipLength > Byte.MAX_VALUE) {
                throw new IllegalArgumentException("ip too long");
            }
            out.writeByte(ipLength);
            out.writeBytes(header.getIp().getBytes());
        } else {
            out.writeByte(0);
        }
    }

    /**
     * decode the DefaultProxyHead from in buf
     */
    @Override
    public DefaultProxyHead decodeProxyHead(ByteBuf in, int length) {

        //读取用户id
        long userId = in.readLong();
        //读取session
        String session = null;
        byte s = in.readByte();
        if (s > 0) {
            byte[] sBuf = new byte[s];
            in.readBytes(sBuf);
            session = new String(sBuf);
        }

        //读取ip
        String ip = null;
        s = in.readByte();
        if (s > 0) {
            byte[] sBuf = new byte[s];
            in.readBytes(sBuf);
            ip = new String(sBuf);
        }
        return new DefaultProxyHead(session, ip, userId);
    }
}
