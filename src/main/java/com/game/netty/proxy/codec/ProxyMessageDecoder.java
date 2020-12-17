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

import com.game.netty.client.ClientMessage;
import com.game.netty.proxy.ProxyMessage;
import com.game.netty.util.ClientMessageCodecUtil;
import com.game.netty.util.GameByteBufAlloc;

import com.game.netty.client.codec.MessageBodyCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * ProxyMessage Decoder for Netty Channel pipeline
 *
 * @param <H> ProxyHead type
 * @param <B> ClientMessage Body type
 */
public class ProxyMessageDecoder<H, B> extends ByteToMessageDecoder {

    private final MessageBodyCodec<B> bodyCodec;
    private final ProxyHeaderCodec<H> headerCodec;

    /**
     * client message header size
     */
    private final int clientMessageHeadSize;

    /**
     * should the decoded ProxyMessage.clientMessage.buf keep the raw byte buf
     */
    private final boolean keepMessageBuf;

    /**
     * @param headerCodec           proxy header codec
     * @param bodyCodec             message body codec
     * @param clientMessageHeadSize client message head size
     * @param keepMessageBuf        is ProxyMessage.clientMessage.buf keep the raw byte buf
     */
    public ProxyMessageDecoder(ProxyHeaderCodec<H> headerCodec, MessageBodyCodec<B> bodyCodec, int clientMessageHeadSize, boolean keepMessageBuf) {
        this.bodyCodec = bodyCodec;
        this.headerCodec = headerCodec;
        this.clientMessageHeadSize = clientMessageHeadSize;
        this.keepMessageBuf = keepMessageBuf;
        if (getClass().getAnnotation(Sharable.class) != null) {
            throw new IllegalStateException("BackendUpSocketMessageDecoder can't be sharable");
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {

        while (in.readableBytes() > Integer.BYTES) {
            int fullLength = in.getInt(in.readerIndex()) + Integer.BYTES;
            if (in.readableBytes() < fullLength) {
                return;
            }
            //skip length field
            in.skipBytes(Integer.BYTES);

            //decode proxy header
            int headSize = in.readInt();
            H proxyHead = headerCodec.decodeProxyHead(in, headSize);

            //decode client header
            int clientStart = in.readerIndex();
            int bodyLength = in.readInt() - clientMessageHeadSize;
            long clientHead = ClientMessageCodecUtil.readHead(in, clientMessageHeadSize);

            //decode inner client message
            int savedReadIndex = in.readerIndex();
            ClientMessage<B> clientMessage;
            if (keepMessageBuf) {
                int clientFullLength = bodyLength + clientMessageHeadSize + Integer.BYTES;
                ByteBuf messageBuf = GameByteBufAlloc.heapBuf(ctx.alloc(), clientFullLength);
                messageBuf.writeBytes(in, clientStart, clientFullLength);
                in.skipBytes(bodyLength);
                //set readIndex to body position
                messageBuf.skipBytes(clientMessageHeadSize + Integer.BYTES);
                B b = bodyCodec.decodeMessageBody(clientHead, messageBuf, bodyLength);
                messageBuf.readerIndex(0);
                clientMessage = new ClientMessage<>(clientHead, messageBuf, b);
            } else {
                B b = bodyCodec.decodeMessageBody(clientHead, in, bodyLength);
                if (b == null) {
                    throw new IllegalStateException("keepMessageBuf=false and decoded body==null, I guess a misconfiguration");
                }
                in.readerIndex(savedReadIndex + bodyLength);
                clientMessage = new ClientMessage<>(clientHead, b);
            }
            ProxyMessage<H, B> proxyMessage = new ProxyMessage<>(proxyHead, clientMessage);
            out.add(proxyMessage);
        }
    }
}
