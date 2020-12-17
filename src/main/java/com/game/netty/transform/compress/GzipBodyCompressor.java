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
 * A BodyTransformer implementation for gzip compress
 */
public class GzipBodyCompressor implements BodyTransformer {

    @FunctionalInterface
    public interface CompressPolicy {
        /**
         * A policy allow you to indicate weather the message needs compress, via message head
         * This is something like http "Content-Encoding" strategy
         * @param messageHead  head of client message
         * @return compress message body or not
         */
        boolean needCompress(long messageHead);
    }

    private CompressPolicy policy = messageHead -> true;

    public GzipBodyCompressor() {

    }

    public GzipBodyCompressor(CompressPolicy policy) {
        this.policy = policy;
    }

    @Override
    public void transformBody(long messageHead, ByteBuf bodyBuf) {
        if (!policy.needCompress(messageHead)) {
            return;
        }
        try {
            CompressUtil.handleCompress(bodyBuf, true);
        } catch (Exception e) {
            throw new RuntimeException("compress body error", e);
        }
    }
}
