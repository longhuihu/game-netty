package com.game.netty.sample.websocket;

import com.game.netty.client.acceptor.SslUtil;
import com.game.netty.sample.common.Constant;
import com.game.netty.sample.common.SampleWSServer;

import javax.net.ssl.SSLContext;

/**
 * websocket server part
 * You can using http://www.websocket-test.com/ as client to test it
 */
public class WSServerStarter {

    public static void main(String[] args) throws Exception {
        SampleWSServer wsServer = new SampleWSServer("wsServer");
        //url: ws://127.0.0.1:9669/game
        wsServer.start(Constant.WS_PORT, "/game", null);
    }
}
