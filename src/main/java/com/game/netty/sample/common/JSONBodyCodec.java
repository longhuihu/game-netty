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

package com.game.netty.sample.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.game.netty.client.codec.MessageBodyCodec;
import io.netty.buffer.ByteBuf;

public class JSONBodyCodec implements MessageBodyCodec<JSONObject> {

    @Override
    public int bodyEncodeSize(JSONObject message) {
        return message.toJSONString().getBytes().length;
    }

    @Override
    public void encodeBody(JSONObject body, ByteBuf out) {
        out.writeBytes(body.toJSONString().getBytes());
    }

    @Override
    public JSONObject decodeMessageBody(long messageHead, ByteBuf bodyBuf, int length) {
        byte[] data = new byte[length];
        bodyBuf.readBytes(data);
        return JSON.parseObject(new String(data));
    }
}
