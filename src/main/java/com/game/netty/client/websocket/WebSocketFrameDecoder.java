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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Decoder for Netty Channel Pipeline, if the client connect proxy using web socket
 */
@ChannelHandler.Sharable
public class WebSocketFrameDecoder<B> extends MessageToMessageDecoder<WebSocketFrame> {

    /**
     * @see ClientMessage head size
     */
    private final int messageHeadSize;

    /**
     * @see ClientMessage body codec
     */
    private final MessageBodyCodec<B> delegate;

    /**
     * is there a length field, as web socket has already split byte sequence into frames
     */
    private final boolean hasLengthField;

    public WebSocketFrameDecoder(MessageBodyCodec<B> delegate, int messageHeadSize, boolean hasLengthField) {
        this.messageHeadSize = messageHeadSize;
        this.delegate = delegate;
        this.hasLengthField = hasLengthField;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame msg, List out) throws Exception {
        if (msg instanceof BinaryWebSocketFrame) {
            ByteBuf content = msg.content();
            try {
                content.resetReaderIndex();
                if (hasLengthField) {
                    int length = content.readInt();
                    if (length != content.readableBytes()) {
                        throw new IllegalStateException("length!=readableBytes, client encode error");
                    }
                }
                int lengthFieldValue = content.readableBytes();
                long messageHead = ClientMessageCodecUtil.readHead(content, messageHeadSize);
                B bodyObject = delegate.decodeMessageBody(messageHead, content, content.readableBytes());
                content.resetReaderIndex();

                if (hasLengthField) {
                    ClientMessage<B> clientMsg = new ClientMessage<>(messageHead, content, bodyObject);
                    out.add(clientMsg);
                } else {
                    //如果websocket编码没有lengthField，为满足ClientMessage需求，增加一个lengthField
                    ByteBuf lengthBuf = GameByteBufAlloc.heapBuf(ctx.alloc(), Integer.BYTES);
                    lengthBuf.writeInt(lengthFieldValue);
                    ByteBuf messageBuf = Unpooled.wrappedBuffer(2, lengthBuf, content);
                    ClientMessage<B> clientMsg = new ClientMessage<>(messageHead, messageBuf, bodyObject);
                    out.add(clientMsg);
                }
            } catch (Exception e) {
                exceptionCaught(ctx, e);
            }
        } else {
            ByteBuf buf = msg.content();
            // if text mode, there's no head
            B body = delegate.decodeMessageBody(0, buf, buf.readableBytes());
            ClientMessage<B> clientMsg = new ClientMessage<>(0, buf.retain(), body);
            out.add(clientMsg);
        }
    }
}
