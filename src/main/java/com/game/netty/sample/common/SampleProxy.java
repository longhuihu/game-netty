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
import com.game.netty.client.acceptor.ClientAcceptedChannel;
import com.game.netty.client.acceptor.ClientAcceptorDelegate;
import com.game.netty.client.acceptor.ClientSocketAcceptor;
import com.game.netty.client.acceptor.session.AbstractSessionManager;
import com.game.netty.client.acceptor.session.DefaultSession;
import com.game.netty.client.acceptor.session.SessionInterface;
import com.game.netty.client.acceptor.session.SessionManagerInterface;
import com.game.netty.config.ClientChannelConfig;
import com.game.netty.config.ProxyChannelConfig;
import com.game.netty.config.codec.ClientCodecConfig;
import com.game.netty.config.codec.ProxyCodecConfig;
import com.game.netty.client.ClientMessage;
import com.game.netty.proxy.ProxyMessage;
import com.game.netty.ServerDefine;
import com.game.netty.proxy.connector.ProxyConnectChannel;
import com.game.netty.proxy.connector.ProxyConnectorDelegate;
import com.game.netty.proxy.connector.ProxyConnector;
import com.game.netty.transform.compress.GzipBodyUncompressor;
import com.game.netty.transform.encrypt.RC4BodyEnDecryptor;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * represent proxy server, communicate with client via socket, hide logic server from outer network
 */
public class SampleProxy {

    private  ClientSocketAcceptor clientSocketAcceptor;
    private  SessionManagerInterface<DefaultSession> sessionManager;

    private  ProxyConnector proxyConnector;

    private  final Random random = new Random();
    private final String proxyName;

    public SampleProxy(String proxyName) {
        this.proxyName = proxyName;
    }

    public void start(int listenPort, List<ServerDefine> logicServers) throws Exception {
        initServerConnector(logicServers);
        initClientAcceptor(listenPort,logicServers);
    }

    public  void initClientAcceptor(int listenPort,List<ServerDefine> logicServers) throws Exception {

        ClientAcceptorDelegate<JSONObject> delegate = new ClientAcceptorDelegate<JSONObject>() {

            @Override
            public void onAcceptorStarted() {
                LogUtil.print(proxyName,"acceptor started");
            }

            @Override
            public void onChannelStatusEvent(ClientAcceptedChannel channel, ChannelEvent event) {
                LogUtil.print(proxyName,"channel from " + channel + " event:" + event);
            }

            @Override
            public void onChannelExceptionCaught(ClientAcceptedChannel channel, Throwable e) {
                LogUtil.print(proxyName,"channel from" + channel + "exception:" + e);
            }

            @Override
            public void onChannelMessage(ClientAcceptedChannel channel, ClientMessage<JSONObject> message) {

                //add a head to the message from client
                String proxyHeader = channel.getSession().sessionId();
                ProxyMessage<String, JSONObject> proxyMessage = new ProxyMessage<>(proxyHeader, message.retain());

                //randomly forward message to a logic server
                int index = random.nextInt(logicServers.size());
                ServerDefine serverDefine = logicServers.get(index);
                proxyConnector.getChannel(serverDefine.getServerId()).write(proxyMessage);
                LogUtil.print(proxyName,"receive client message, forward  to server" + serverDefine.getServerId());
                LogUtil.print();
            }
        };

        sessionManager = new AbstractSessionManager<DefaultSession>() {
            @Override
            public DefaultSession createSessionForNewChannel(ClientAcceptedChannel channel) {
                String session = UUID.randomUUID().toString();
                return new DefaultSession(session, channel);
            }

            @Override
            protected void onSessionRemoved(DefaultSession defaultSession) {
                //do nothing
            }
        };

        clientSocketAcceptor = new ClientSocketAcceptor(listenPort, delegate);

        //same config with SampleClient
        ClientCodecConfig codecConfig = new ClientCodecConfig(Long.BYTES, new JSONBodyCodec());
        codecConfig.setDecoderKeepMessageBuf(true);

        //reverse config with SampleClient
        codecConfig.addBodyDecodeTransformer(new RC4BodyEnDecryptor("RC4EncryptKeyIsARandomString"));
        codecConfig.addBodyDecodeTransformer(new GzipBodyUncompressor());

        //start to listen
        clientSocketAcceptor.initChannel(new ClientChannelConfig())
                .initCodec(codecConfig)
                .initSessionManager(sessionManager)
                .start();
    }

    /**
     * connect to all logic servers, prepare for message forwarding
     */
    private  void initServerConnector(List<ServerDefine> logicServers) {

        ProxyConnectorDelegate<String, JSONObject> channelEventDelegate = new ProxyConnectorDelegate<String, JSONObject>() {
            @Override
            public void onConnectorStart() {
                LogUtil.print(proxyName,"connector started");
            }

            @Override
            public void onChannelStatusEvent(ProxyConnectChannel channel, ChannelEvent event) {
                LogUtil.print(proxyName,"channel to " + channel + " event:" + event);
            }

            @Override
            public void onChannelExceptionCaught(ProxyConnectChannel channel, Throwable e) {
                LogUtil.print(proxyName,"channel to" + channel + "exception:" + e);
            }

            @Override
            public void onChannelMessage(ProxyConnectChannel channel, ProxyMessage<String, JSONObject> socketMessage) {

                //via session, proxy identify the client that the message is for
                String sessionId = socketMessage.proxyHeader();
                LogUtil.print(proxyName,"receive message from server" + channel.getRemoteServer().getServerId()
                        + ", forward to client with session: " + socketMessage.proxyHeader());
                LogUtil.print();

                SessionInterface session = sessionManager.getSession(sessionId);
                if (session != null) {
                    ClientMessage<JSONObject> retain = socketMessage.clientMessage().retain();
                    session.channel().writeAndFlush(retain);
                }
            }
        };

        proxyConnector = new ProxyConnector(channelEventDelegate);


        ProxyChannelConfig config = new ProxyChannelConfig();
        ProxyCodecConfig codecConfig = new ProxyCodecConfig(Long.BYTES, new SampleProxyHeaderCodec(), new JSONBodyCodec());
        codecConfig.setDecoderKeepMessageBuf(true);

        config.setChannelReadTimeOut(0);
        proxyConnector.initChannel(config)
                .initCodec(codecConfig)
                .start(logicServers);
    }
}
