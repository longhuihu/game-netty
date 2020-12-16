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

package com.game.netty.transform.encrypt;

import com.game.netty.transform.BodyTransformer;
import io.netty.buffer.ByteBuf;

/**
 * A BodyTransformer implementation for rc4 encrypt.
 * Attention! rc4 encrypt and decrypt is same
 */
public class RC4BodyEnDecryptor implements BodyTransformer {

    private final RC4Util rc4New;

    public RC4BodyEnDecryptor(String rc4Key) {
        rc4New = new RC4Util(rc4Key);
    }

    @Override
    public void transformBody(long messageHead, ByteBuf bodyBuf) {
        byte[] buf;
        int offset = 0;
        int length = bodyBuf.readableBytes();
        int readIndex = bodyBuf.readerIndex();

        //rc4算法不会改变数据长度
        if (!bodyBuf.hasArray()) {
            //bodyBuf底层不是byte array，创建一份copy，执行算法，再写回
            buf = new byte[length];
            bodyBuf.getBytes(readIndex, buf);
            rc4New.rc4(buf);
            bodyBuf.readerIndex(readIndex);
            bodyBuf.writerIndex(readIndex);
            bodyBuf.writeBytes(buf);
        } else {
            //bodyBuf底层是byte array，算法直接在底层byte array上执行即可，效率更高
            buf = bodyBuf.array();
            offset = bodyBuf.arrayOffset() + readIndex;
            rc4New.rc4(buf, offset, length);
        }
    }
}
