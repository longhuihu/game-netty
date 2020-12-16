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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @param <B> Client Message Body type
 * @see ClientMessage Encoder for Netty Channel Pipeline.
 * * It's composed by two parts: BodyTransformer and MessageBodyCodec.
 * <p>
 * The ClientMessage byte format is [length(int)]+[head(1 or 2 or 4 or 8 byte)]+[body]
 * <p>
 * There may be one or more BodyTransformer, transforming bytes of body to implement encrypting, compressing etc.
 * <p>
 * MessageBodyCodec decode body bytes to body object after BodyTransformers have been applied.
 */
public class ClientMessageDecoder<B> extends ByteToMessageDecoder {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(ClientMessageDecoder.class);

    private final MessageBodyCodec<B> bodyCodec;

    /**
     * max body size, for malicious client
     */
    private int maxBodySize = 10 * 1024;

    /**
     * client message head size
     */
    private final int messageHeadSize;

    /**
     * BodyTransformer list, would apply orderly
     */
    private final List<BodyTransformer> transformers = new ArrayList<>();

    /**
     * whether keep  byteBuf of body in decoded ClientMessage
     */
    private final boolean keepMessageBuf;


    public ClientMessageDecoder(MessageBodyCodec<B> bodyCodec, int messageHeadSize, boolean keepMessageBuf) {
        this.messageHeadSize = messageHeadSize;
        this.bodyCodec = bodyCodec;
        this.keepMessageBuf = keepMessageBuf;
    }

    public void setMaxBodySize(int maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    public void addTransformer(BodyTransformer transformer) {
        transformers.add(transformer);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {

        while (in.readableBytes() > Integer.BYTES) {
            int fullLength = in.getInt(in.readerIndex()) + Integer.BYTES;
            if (!checkMessageLimit(messageHeadSize, in, ctx)
                    || in.readableBytes() < fullLength) {
                return;
            }

            //decode client message head
            int startIndex = in.readerIndex();
            in.skipBytes(Integer.BYTES);
            long messageHead = ClientMessageCodecUtil.readHead(in, messageHeadSize);

            int bodyLength = fullLength - messageHeadSize - Integer.BYTES;

            /* if keepMessageBuf, ClientMessage.buf should be a independent&extendable ByteBuf，for future reuse.
             * if transformers.size()>0, we need a heap based ByteBuf for BodyTransformers.
             * So, we create a new heap byteBuf at under neither condition by KISS principle，do not pursue max performance
             */
            ByteBuf messageBuf;
            if (keepMessageBuf || !transformers.isEmpty()) {
                messageBuf = GameByteBufAlloc.heapBuf(ctx.alloc(), fullLength);
                messageBuf.writeBytes(in, startIndex, fullLength);
                TransformerRunner.runTransformers(transformers, messageBuf, messageHead, messageHeadSize);
            } else {
                messageBuf = in.retainedSlice(startIndex, fullLength);
            }
            //skip body, as has been read into messageBuf above
            in.skipBytes(bodyLength);

            //set messageBuf.readIndex to body position
            messageBuf.skipBytes(Integer.BYTES + messageHeadSize);
            B bodyObject = bodyCodec.decodeMessageBody(messageHead, messageBuf, messageBuf.readableBytes());
            //roll back messageBuf.readIndex
            messageBuf.readerIndex(0);

            //create ClientMessage
            ClientMessage<B> clientMsg = new ClientMessage<>(messageHead, keepMessageBuf ? messageBuf : null, bodyObject);
            out.add(clientMsg);

            if (!keepMessageBuf) {
                messageBuf.release();
            }
        }
    }

    /**
     * return false means:
     * 1. there is a bug in this decoder
     * 2. messageLengthLimit is too small
     * 3. client send wrong data
     * 4. there are malicious client
     */
    private boolean checkMessageLimit(long messageSize, ByteBuf in, ChannelHandlerContext ctx) {
        if (messageSize > maxBodySize) {
            //here read msgId just for logging
            long messageHead = 0;
            if (in.readableBytes() >= Integer.BYTES + messageHeadSize) {
                messageHead = ClientMessageCodecUtil.getHead(in, in.readerIndex() + Integer.BYTES, messageHeadSize);
            }
            logger.warn("message:{} body is too large:{} from:{}", messageHead, messageSize, ctx.channel().remoteAddress());

            //omit following bytes and close channel
            in.skipBytes(in.readableBytes());
            ctx.close();
            return false;
        }
        return true;
    }
}
