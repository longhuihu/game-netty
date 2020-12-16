package com.game.netty.sample.common;

import com.game.netty.ChannelEvent;
import com.game.netty.client.ClientMessage;
import com.game.netty.client.acceptor.ClientAcceptedChannel;
import com.game.netty.client.acceptor.ClientAcceptorDelegate;
import com.game.netty.client.websocket.ClientWebSocketAcceptor;
import com.game.netty.client.acceptor.session.AbstractSessionManager;
import com.game.netty.client.acceptor.session.DefaultSession;
import com.game.netty.client.acceptor.session.SessionManagerInterface;
import com.game.netty.config.ClientChannelConfig;
import com.game.netty.config.codec.ClientCodecConfig;

import javax.net.ssl.SSLContext;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class SampleWSServer {

    private ClientWebSocketAcceptor webSocketAcceptor;
    private SessionManagerInterface<DefaultSession> sessionManager;

    private final String serverName;

    public SampleWSServer(String serverName) {
        this.serverName = serverName;
    }

    public void start(int listenPort, String path, SSLContext sslContext) throws Exception {

        ClientAcceptorDelegate<String> delegate = new ClientAcceptorDelegate<String>() {

            @Override
            public SSLContext createSSLContext() {
                return sslContext;
            }

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
            public void onChannelMessage(ClientAcceptedChannel channel, ClientMessage<String> message) {

                LogUtil.print(serverName, "receive message:" + message.getBody());

                ClientMessage<String> response = new ClientMessage<>(serverName + " response to:" + message.getBody());
                channel.writeAndFlush(response);

                //show how to push message to client
                if (ThreadLocalRandom.current().nextFloat() > 0.5) {
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

        webSocketAcceptor = new ClientWebSocketAcceptor(listenPort, path, true, delegate);

        //same config with SampleClient
        ClientCodecConfig codecConfig = new ClientCodecConfig(0, new StringCodec());
        codecConfig.setDecoderKeepMessageBuf(false);

        //start to listen
        webSocketAcceptor.initChannel(new ClientChannelConfig())
                .initCodec(codecConfig)
                .initSessionManager(sessionManager)
                .start();
    }

    private void pushToClient(ClientAcceptedChannel channel) {
        String pushContent = serverName + " push message " + ThreadLocalRandom.current().nextInt();
        LogUtil.print(serverName, pushContent);

        channel.writeAndFlush(new ClientMessage<>(0, pushContent));
    }
}
