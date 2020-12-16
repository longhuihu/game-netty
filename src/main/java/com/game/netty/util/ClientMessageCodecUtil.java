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
package com.game.netty.util;

import io.netty.buffer.ByteBuf;


/**
 * tool for client message codec
 */
public class ClientMessageCodecUtil {

    /**
     * read head from byteBuf-in，the read index of in will change
     *
     * @param in       input byte buf
     * @param headSize head size
     * @return head  as long value
     */
    public static long readHead(ByteBuf in, int headSize) {
        switch (headSize) {
            case 0:
                return 0L;
            case Byte.BYTES:
                return in.readByte();
            case Short.BYTES:
                return in.readShort();
            case Integer.BYTES:
                return in.readInt();
            case Long.BYTES:
                return in.readLong();
            default:
                throw new IllegalArgumentException("not supported headSize: " + headSize);
        }
    }

    /**
     * read head from byteBuf-in，the read index of in will not change
     *
     * @param in       input byte buf
     * @param headSize head size
     * @return head  as long value
     */
    public static long getHead(ByteBuf in, int readIndex, int headSize) {
        switch (headSize) {
            case 0:
                return 0L;
            case Byte.BYTES:
                return in.getByte(readIndex);
            case Short.BYTES:
                return in.getShort(readIndex);
            case Integer.BYTES:
                return in.getInt(readIndex);
            case Long.BYTES:
                return in.getLong(readIndex);
            default:
                throw new IllegalArgumentException("not supported headSize: " + headSize);
        }
    }


    /**
     * write head value in to byteBuf-in
     *
     * @param in       byteBuf
     * @param value    head value as long
     * @param headSize head size
     */
    public static void writeHead(ByteBuf in, long value, int headSize) {
        switch (headSize) {
            case 0:
                break;
            case Byte.BYTES:
                in.writeByte((short) value);
                break;
            case Short.BYTES:
                in.writeShort((short) value);
                break;
            case Integer.BYTES:
                in.writeInt((int) value);
                break;
            case Long.BYTES:
                in.writeLong(value);
                break;
            default:
                throw new IllegalArgumentException("not supported headSize: " + headSize);
        }
    }

    private ClientMessageCodecUtil() {
    }
}
