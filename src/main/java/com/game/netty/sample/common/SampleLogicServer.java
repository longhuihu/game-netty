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
import com.game.netty.sample.common.JSONBodyCodec;
import com.game.netty.sample.common.SampleProxyHeaderCodec;
import com.game.netty.ChannelEvent;
import com.game.netty.config.ProxyChannelConfig;
import com.game.netty.config.codec.ProxyCodecConfig;
import com.game.netty.client.ClientMessage;
import com.game.netty.proxy.ProxyMessage;
import com.game.netty.proxy.acceptor.ProxyAcceptorDelegate;
import com.game.netty.proxy.acceptor.ProxyAcceptedChannel;
import com.game.netty.proxy.acceptor.ProxyAcceptor;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * represent game logic server, probably work in the inner environment, accept socket link only from proxy server
 */
public class SampleLogicServer {

    private final String serverName;

    public SampleLogicServer(String serverName) {
        this.serverName = serverName;
    }

    public void start(int serverId, int port) throws InterruptedException {

        ProxyAcceptorDelegate<String, JSONObject> delegate = new ProxyAcceptorDelegate<String, JSONObject>() {

            @Override
            public void onAcceptorStarted() {
                LogUtil.print(serverName, "acceptor started");
            }

            @Override
            public void onChannelStatusEvent(ProxyAcceptedChannel channel, ChannelEvent event) {
                LogUtil.print(serverName, "channel from " + channel.getChannel().remoteAddress() + " event:" + event);
            }

            @Override
            public void onChannelExceptionCaught(ProxyAcceptedChannel channel, Throwable e) {
                LogUtil.print(serverName, "channel from" + channel.getChannel().remoteAddress() + "exception:" + e);
            }

            @Override
            public void onChannelMessage(ProxyAcceptedChannel channel, ProxyMessage<String, JSONObject> message) {

                LogUtil.print(serverName, "receive message proxy header:" + message.proxyHeader());
                LogUtil.print(serverName, "receive message body:" + message.clientMessage().getBody());
                JSONObject body = message.clientMessage().getBody();
                String clientSay = body.getString("clientSay");

                //make a response to client
                JSONObject respBody = new JSONObject();
                respBody.put(serverName, "response to:" + clientSay);
                ClientMessage<JSONObject> response = new ClientMessage<>(0, respBody);

                //tell gate which session this response is for
                ProxyMessage<String, JSONObject> proxyMessage = new ProxyMessage<>(message.proxyHeader(), response);
                channel.writeAndFlush(proxyMessage);

                //show how to push message to client
                if (ThreadLocalRandom.current().nextFloat() > 0.8) {
                    channel.getChannel().eventLoop().schedule(() -> {
                        pushToClient(channel, message.proxyHeader());
                    }, 3, TimeUnit.SECONDS);
                }
            }
        };

        //start listener for proxy
        ProxyAcceptor acceptor = new ProxyAcceptor(port, delegate);
        ProxyChannelConfig config = new ProxyChannelConfig();
        config.setChannelReadTimeOut(0);
        acceptor.initChannel(config)
                .initCodec(new ProxyCodecConfig(Long.BYTES, new SampleProxyHeaderCodec(), new JSONBodyCodec()));

        acceptor.start();
    }

    private void pushToClient(ProxyAcceptedChannel channel, String sessionId) {
        JSONObject respBody = new JSONObject();
        String pushContent = "push message" + ThreadLocalRandom.current().nextInt();
        respBody.put(serverName, pushContent);
        LogUtil.print(serverName, pushContent);

        ProxyMessage<String, JSONObject> proxyMessage = new ProxyMessage<>(sessionId, new ClientMessage<>(0, respBody));
        channel.writeAndFlush(proxyMessage);
    }

}
