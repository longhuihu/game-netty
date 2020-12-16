package com.game.netty.sample.common;

import com.alibaba.fastjson.JSONObject;
import com.game.netty.ChannelEvent;
import com.game.netty.client.ClientMessage;
import com.game.netty.client.acceptor.ClientAcceptedChannel;
import com.game.netty.client.acceptor.ClientAcceptorDelegate;
import com.game.netty.client.acceptor.ClientSocketAcceptor;
import com.game.netty.client.acceptor.session.AbstractSessionManager;
import com.game.netty.client.acceptor.session.DefaultSession;
import com.game.netty.client.acceptor.session.SessionManagerInterface;
import com.game.netty.config.ClientChannelConfig;
import com.game.netty.config.codec.ClientCodecConfig;
import com.game.netty.transform.compress.GzipBodyUncompressor;
import com.game.netty.transform.encrypt.RC4BodyEnDecryptor;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class SampleServer {

    private ClientSocketAcceptor clientSocketAcceptor;
    private SessionManagerInterface<DefaultSession> sessionManager;

    private final String serverName;

    public SampleServer(String serverName) {
        this.serverName = serverName;
    }

    public void start(int listenPort) throws Exception {

        ClientAcceptorDelegate<JSONObject> delegate = new ClientAcceptorDelegate<JSONObject>() {
            @Override
            public void onAcceptorStarted() {
                LogUtil.print(serverName, "acceptor started");
            }

            @Override
            public void onChannelStatusEvent(ClientAcceptedChannel channel, ChannelEvent event) {
                LogUtil.print(serverName, "channel from " + channel + " event:" + event);
            }

            @Override
            public void onChannelExceptionCaught(ClientAcceptedChannel channel, Throwable e) {
                LogUtil.print(serverName, "channel from" + channel + "exception:" + e);
            }

            @Override
            public void onChannelMessage(ClientAcceptedChannel channel, ClientMessage<JSONObject> message) {

                LogUtil.print(serverName, "receive message:" + message.getBody());
                JSONObject body = message.getBody();
                String clientSay = body.getString("clientSay");

                //make a response to client
                JSONObject respBody = new JSONObject();
                respBody.put(serverName, "response to:" + clientSay);
                ClientMessage<JSONObject> response = new ClientMessage<>(0, respBody);
                channel.writeAndFlush(response);

                //show how to push message to client
                if (ThreadLocalRandom.current().nextFloat() > 0.8) {
                    channel.getChannel().eventLoop().schedule(() -> {
                        pushToClient(channel);
                    }, 3, TimeUnit.SECONDS);
                }
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
        codecConfig.setDecoderKeepMessageBuf(false);

        //reverse config with SampleClient
        codecConfig.addBodyDecodeTransformer(new RC4BodyEnDecryptor("RC4EncryptKeyIsARandomString"));
        codecConfig.addBodyDecodeTransformer(new GzipBodyUncompressor());

        //start to listen
        clientSocketAcceptor.initChannel(new ClientChannelConfig())
                .initCodec(codecConfig)
                .initSessionManager(sessionManager)
                .start();
    }

    private void pushToClient(ClientAcceptedChannel channel) {
        JSONObject respBody = new JSONObject();
        String pushContent = "push message" + ThreadLocalRandom.current().nextInt();
        respBody.put(serverName, pushContent);
        LogUtil.print(serverName, pushContent);

        channel.writeAndFlush(new ClientMessage<>(0, respBody));
    }
}
