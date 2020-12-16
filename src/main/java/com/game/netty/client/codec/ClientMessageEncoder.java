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
import com.game.netty.transform.BodyTransformer;
import com.game.netty.transform.TransformerRunner;
import com.game.netty.util.ClientMessageCodecUtil;
import com.game.netty.util.GameByteBufAlloc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @see ClientMessage Encoder for Netty Channel Pipeline.
 * * It's composed by two parts: BodyTransformer and MessageBodyCodec.
 *
 * The ClientMessage byte format is [length(int)]+[head(1 or 2 or 4 or 8 byte)]+[body]
 *
 * MessageBodyCodec encode body to byteBuf, then apply all BodyTransformers to body orderly.
 *
 * There may be one or more BodyTransformer, transforming bytes of body to implement encrypting, compressing etc.
 *
 * @param <B> Client Message Body type
 */
@ChannelHandler.Sharable
public class ClientMessageEncoder<B> extends ChannelOutboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClientMessageEncoder.class);

    /**
     * message body codec
     */
    private final MessageBodyCodec<B> delegate;

    /**
     * client message head size
     */
    private final int messageHeadSize;

    /**
     * BodyTransformer list, would apply orderly
     */
    private final List<BodyTransformer> transformers = new ArrayList<>();

    public ClientMessageEncoder(MessageBodyCodec<B> delegate, int messageHeadSize) {
        this.delegate = delegate;
        this.messageHeadSize = messageHeadSize;
    }

    public void addTransformer(BodyTransformer transformer) {
        transformers.add(transformer);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof ByteBuf) {
            ctx.write(msg, promise);
        } else {
            ByteBuf byteBuf = encode(ctx.alloc(), msg);
            if (byteBuf != null) {
                ctx.write(byteBuf, promise);
            }
        }
    }

    public final ByteBuf encode(ByteBufAllocator ctxAllocator, Object msg) {
        ByteBuf buf;
        long messageHead;
        if (msg instanceof ClientMessage) {
            ClientMessage<B> message = (ClientMessage<B>) msg;
            messageHead = message.getHead();
            try {
                if (message.content() != null) {
                    buf = message.content().retain();
                } else {
                    buf = encodeFromBody(ctxAllocator, message, messageHead);
                }
            } catch (Exception e) {
                throw e;
            } finally {
                message.release();
            }
        } else {
            logger.error("message type not supported: {} ", msg.getClass());
            return null;
        }
        TransformerRunner.runTransformers(transformers, buf, messageHead, messageHeadSize);
        return buf;
    }

    private ByteBuf encodeFromBody(ByteBufAllocator ctxAllocator, ClientMessage<B> message, long messageHead) {
        int bodySize = delegate.bodyEncodeSize(message.getBody());
        int bufSize = Integer.BYTES + messageHeadSize + bodySize;
        ByteBuf buf = GameByteBufAlloc.heapBuf(ctxAllocator, bufSize);
        buf.writeInt(bodySize + messageHeadSize);
        ClientMessageCodecUtil.writeHead(buf, messageHead, messageHeadSize);
        delegate.encodeBody(message.getBody(), buf);
        return buf;
    }
}
