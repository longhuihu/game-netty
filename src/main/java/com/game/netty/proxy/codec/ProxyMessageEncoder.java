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

import com.game.netty.proxy.ProxyMessage;
import com.game.netty.util.ClientMessageCodecUtil;
import com.game.netty.util.GameByteBufAlloc;
import com.game.netty.client.codec.MessageBodyCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * ProxyMessage Encoder for Netty Channel pipeline;
 * The encode format is:  [FullLength(Int)]+[ProxyHeadSize(Int)]+[ProxyHead Bytes]+[ClientMessage Encoded]
 *
 * @param <H> Proxy Header Type
 * @param <B> Message Body Type
 */
@ChannelHandler.Sharable
public class ProxyMessageEncoder<H, B> extends ChannelOutboundHandlerAdapter {

    private final int clientMessageHeadSize;
    private final MessageBodyCodec<B> bodyCodec;
    private final ProxyHeaderCodec<H> headerCodec;

    /**
     * @param headerCodec           proxy header codec
     * @param bodyCodec             message body codec
     * @param clientMessageHeadSize client message head size
     */
    public ProxyMessageEncoder(ProxyHeaderCodec<H> headerCodec, MessageBodyCodec<B> bodyCodec, int clientMessageHeadSize) {
        this.clientMessageHeadSize = clientMessageHeadSize;
        this.bodyCodec = bodyCodec;
        this.headerCodec = headerCodec;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {

        if (msg instanceof ProxyMessage) {
            ProxyMessage<H, B> message = (ProxyMessage<H, B>) msg;
            try {
                int serverHeadSize = headerCodec.proxyHeadEncodeSize(message.proxyHeader());

                if (message.clientMessage().content() != null) {
                    //if the inner clientMessage keep the message body buf, reuse it
                    ByteBuf buf = GameByteBufAlloc.heapBuf(ctx.alloc(), serverHeadSize + Integer.BYTES * 2);
                    int clientMessageSize = message.clientMessage().content().readableBytes();
                    buf.writeInt(Integer.BYTES + serverHeadSize + clientMessageSize);
                    buf.writeInt(serverHeadSize);
                    headerCodec.encodeProxyHead(message.proxyHeader(), buf);
                    ByteBuf fullBuf = Unpooled.compositeBuffer(2)
                            .addComponent(true, buf)
                            .addComponent(true, message.clientMessage().content().retain());
                    ctx.write(fullBuf, promise);
                } else {
                    int bodyEncodeSize = bodyCodec.bodyEncodeSize(message.clientMessage().getBody());
                    int clientMessageSize = Integer.BYTES + clientMessageHeadSize + bodyEncodeSize;
                    ByteBuf buf = GameByteBufAlloc.heapBuf(ctx.alloc(), Integer.BYTES * 2 + serverHeadSize + clientMessageSize);

                    buf.writeInt(Integer.BYTES + serverHeadSize + clientMessageSize);
                    buf.writeInt(serverHeadSize);
                    headerCodec.encodeProxyHead(message.proxyHeader(), buf);

                    buf.writeInt(clientMessageSize - Integer.BYTES);
                    ClientMessageCodecUtil.writeHead(buf, message.clientMessage().getHead(), clientMessageHeadSize);
                    bodyCodec.encodeBody(message.clientMessage().getBody(), buf);

                    ctx.write(buf, promise);
                }
            } finally {
                message.release();
            }
        } else {
            ctx.write(msg, promise);
        }
    }
}
