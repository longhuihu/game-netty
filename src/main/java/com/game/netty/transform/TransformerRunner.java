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

package com.game.netty.transform;


import io.netty.buffer.ByteBuf;

import java.util.List;

public class TransformerRunner {

    /**
     * apply transformers on message body
     * @see BodyTransformer
     * @param transformers  transformer list to apply orderly
     * @param msgBuf message body
     * @param messageHead  message head
     * @param headSize message head size
     */
    public static void runTransformers(List<BodyTransformer> transformers, ByteBuf msgBuf, long messageHead, int headSize) {
        int bodyIndex = headSize + Integer.BYTES;
        int msgIndex = msgBuf.readerIndex();

        //move readIndex to body position
        msgBuf.skipBytes(bodyIndex);
        for (BodyTransformer transformer : transformers) {
            int saveReadableBytes = msgBuf.readableBytes();
            int savedReadIndex = msgBuf.readerIndex();
            transformer.transformBody(messageHead, msgBuf);
            if (savedReadIndex != msgBuf.readerIndex()) {
                throw new IllegalStateException("BodyTransformer should not change body buf read index");
            }

            //if the transformer changed the body length，fix the length field
            if (msgBuf.readableBytes() != saveReadableBytes) {
                msgBuf.setInt(msgIndex, msgBuf.readableBytes() + headSize);
            }
        }

        //roll back readIndex position
        msgBuf.readerIndex(msgIndex);
    }

    private TransformerRunner() {

    }
}
