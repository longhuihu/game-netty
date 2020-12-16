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

package com.game.netty.client.websocket;

import com.game.netty.client.ClientMessage;
import com.game.netty.client.codec.MessageBodyCodec;
import com.game.netty.util.ClientMessageCodecUtil;
import com.game.netty.util.GameByteBufAlloc;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

/**
 * Encoder for Netty Channel Pipeline, if the client connect proxy using web socket
 */
@ChannelHandler.Sharable
public class WebSocketFrameEncoder<B> extends MessageToMessageEncoder<Object> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocketFrameEncoder.class);

    /**
     * @see ClientMessage head size
     */
    private final int messageHeadSize;

    /**
     * allow user write a ByteBuf into channel directly
     */
    private final boolean allowByteBuf;

    /**
     * @see ClientMessage body codec
     */
    private final MessageBodyCodec<B> delegate;

    /**
     * encode to TextWebSocketFrame or BinaryWebSocketFrame
     */
    private final boolean textMode;


    public WebSocketFrameEncoder(MessageBodyCodec<B> delegate, int messageHeadSize, boolean textMode) {
        this(delegate, messageHeadSize, textMode, false);
    }

    public WebSocketFrameEncoder(MessageBodyCodec<B> delegate, int messageHeadSize, boolean textMode, boolean allowByteBuf) {
        this.messageHeadSize = messageHeadSize;
        this.textMode = textMode;
        this.allowByteBuf = allowByteBuf;
        this.delegate = delegate;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object o, List<Object> out) {
        ClientMessage<B> socketMessage = (ClientMessage<B>) o;
        long messageHead = socketMessage.getHead();
        ByteBuf buf;
        try {
            ByteBuf messageBuf = socketMessage.content();
            if (messageBuf != null) {
                //web socket编码不需要lengthField，跳过去
                buf = messageBuf.retain();
                buf.skipBytes(Integer.BYTES);
            } else {
                buf = encodeRawMessage(ctx.alloc(), socketMessage.getBody(), messageHead);
            }
        } finally {
            socketMessage.release();
        }
        if (textMode) {
            out.add(new TextWebSocketFrame(buf));
        } else {
            out.add(new BinaryWebSocketFrame(buf));
        }
    }

    private ByteBuf encodeRawMessage(ByteBufAllocator ctxAllocator, B body, long messageHead) {
        int messageSize = delegate.bodyEncodeSize(body) + messageHeadSize;
        ByteBuf buf = GameByteBufAlloc.heapBuf(ctxAllocator, messageSize);
        ClientMessageCodecUtil.writeHead(buf, messageHead, messageHeadSize);
        delegate.encodeBody(body, buf);
        return buf;
    }

    @Override
    public boolean acceptOutboundMessage(Object msg) {
        if (msg instanceof ClientMessage) {
            return true;
        }
        if (msg instanceof ByteBuf) {
            if (allowByteBuf) {
                return true;
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn("ByteBuf write directory to web socket channel, is this you intention？");
                }
            }
        }
        return false;
    }
}
