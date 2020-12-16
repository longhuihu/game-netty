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

package com.game.netty.client;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

/**
 * A message between client and proxy server.
 * <p>
 * ClientMessage is composed by two parts: head and body.
 * The body has two form, buf(byte sequence before decode) and body(java object after decode).
 * <p>
 * These tow forms is for efficiency，as Proxy Server probably forward message(need buf), but don't care body content(not need body)；
 * vice versa，the Logic Server only need decoded body object.
 * <p>
 * During decoding, the decoder may decode body content to object or not, depends on you implementation
 *
 * @param <B> ClientMessage Body type
 */
public class ClientMessage<B> implements ReferenceCounted {

    /**
     * client message head, max 8 bytes. For simplify, only 0,1,2,4,8 bytes are valid option
     */
    private long head;

    /**
     * fully encoded message bytes，buf.readableBytes = [length]-[head]-[body]
     * if encrypt and compress involved, this buf is decrypted and uncompressed bytes
     * Attention, ClientMessage has the ownership of buf
     */
    private final ByteBuf buf;

    /**
     * decoded body object
     */
    private final B body;


    public ClientMessage(long head, B body) {
        this.head = head;
        this.body = body;
        this.buf = null;
    }

    public ClientMessage(long head, ByteBuf buf, B body) {
        this.head = head;
        this.body = body;
        this.buf = buf;
    }

    public ClientMessage(B body) {
        this(0, body);
    }

    public ClientMessage(ByteBuf buf, B body) {
        this(0, buf, body);
    }


    public long getHead() {
        return head;
    }

    public void setHead(long head) {
        this.head = head;
    }

    public <T> T getBody() {
        return (T) body;
    }

    public ByteBuf content() {
        return buf;
    }

    @Override
    public ClientMessage<B> retain() {
        if (buf != null) {
            buf.retain();
        }
        return this;
    }

    @Override
    public ClientMessage<B> retain(int increment) {
        if (buf != null) {
            buf.retain(increment);
        }
        return this;
    }

    @Override
    public ClientMessage<B> touch() {
        if (buf != null) {
            buf.touch();
        }
        return this;
    }

    @Override
    public ClientMessage<B> touch(Object hint) {
        if (buf != null) {
            buf.touch(hint);
        }
        return this;
    }

    @Override
    public int refCnt() {
        return buf == null ? 0 : buf.refCnt();
    }

    @Override
    public boolean release() {
        return buf != null && buf.release();
    }

    @Override
    public boolean release(int decrement) {
        return buf != null && buf.release(decrement);
    }
}
