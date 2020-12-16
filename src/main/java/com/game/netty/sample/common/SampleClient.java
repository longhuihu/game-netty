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

import com.alibaba.fastjson.JSONObject;
import com.game.netty.ChannelEvent;
import com.game.netty.client.connector.ClientConnectorDelegate;
import com.game.netty.client.connector.ClientConnectChannel;
import com.game.netty.client.connector.ClientConnector;
import com.game.netty.config.ClientChannelConfig;
import com.game.netty.config.codec.ClientCodecConfig;
import com.game.netty.client.ClientMessage;
import com.game.netty.ServerDefine;
import com.game.netty.transform.compress.GzipBodyCompressor;
import com.game.netty.transform.encrypt.RC4BodyEnDecryptor;

import java.util.Collections;

/**
 * represent game client, in reality, this would realized by a mobile app or h5 page, not java code.
 */
public class SampleClient {

    private final String clientName;

    public SampleClient(String clientName) {
        this.clientName = clientName;
    }

    public void start(int targetPort) throws InterruptedException {

        ClientConnectorDelegate<JSONObject> delegate = new ClientConnectorDelegate<JSONObject>() {
            @Override
            public void onConnectorStart() {
                LogUtil.print(clientName,"connector started");
            }

            @Override
            public void onChannelStatusEvent(ClientConnectChannel channel, ChannelEvent event) {
                LogUtil.print(clientName,"channel " + channel + " event:" + event);
            }

            @Override
            public void onChannelExceptionCaught(ClientConnectChannel channel, Throwable e) {
                LogUtil.print(clientName,"channel " + channel + " exception:" + e);
            }

            @Override
            public void onChannelMessage(ClientConnectChannel channel, ClientMessage<JSONObject> message) {
                LogUtil.print(clientName,"receive:" + message.getBody().toString());
            }
        };


        //config head size, body codec
        ClientCodecConfig codecConfig = new ClientCodecConfig(Long.BYTES, new JSONBodyCodec());

        //config gzip and rc4， pay attention to the order
        codecConfig.addBodyEncodeTransformer(new GzipBodyCompressor());
        codecConfig.addBodyEncodeTransformer(new RC4BodyEnDecryptor("RC4EncryptKeyIsARandomString"));

        //define target proxy server
        ServerDefine server = new ServerDefine(1, "127.0.0.1", targetPort);

        ClientConnector connector = new ClientConnector(delegate);
        connector.initChannel(new ClientChannelConfig())
                .initCodec(codecConfig)
                .start(Collections.singletonList(server));

        //for simplify, wait connector to establish. you can wait delegate event instead in reality
        Thread.sleep(1000);

        int index = 0;
        while (true) {
            index++;
            JSONObject body = new JSONObject();
            body.put("clientSay", "hello " + index);
            ClientMessage<JSONObject> message = new ClientMessage<>(0, body);

            connector.getChannel(1).writeAndFlush(message);
            LogUtil.print(clientName,"client send message " + index);
            Thread.sleep(500);
            LogUtil.print(clientName,"=======================\n");

            Thread.sleep(5000);
        }
    }
}
