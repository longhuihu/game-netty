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

package com.game.netty.transform.compress;

import com.game.netty.transform.BodyTransformer;
import io.netty.buffer.ByteBuf;

/**
 * A BodyTransformer implementation for gzip uncompress
 */
public class GzipBodyUncompressor implements BodyTransformer {

    @FunctionalInterface
    public interface UncompressPolicy {
        /**
         * A policy allow you to indicate weather the message is compressed, via message head
         * This is something like http "Content-Encoding" strategy
         * @param messageHead  head for ClientMessage
         * @return uncompress message body or not
         */
        boolean needUnCompress(long messageHead);
    }

    private UncompressPolicy policy = messageHead -> true;

    public GzipBodyUncompressor() {

    }

    public GzipBodyUncompressor(UncompressPolicy policy) {
        this.policy = policy;
    }

    @Override
    public void transformBody(long messageHead, ByteBuf bodyBuf) {
        if (!policy.needUnCompress(messageHead)) {
            return;
        }
        try {
            CompressUtil.handleCompress(bodyBuf, false);
        } catch (Exception e) {
            throw new RuntimeException("uncompress body error", e);
        }
    }
}
