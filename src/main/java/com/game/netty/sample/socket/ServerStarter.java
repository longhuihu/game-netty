package com.game.netty.sample.socket;

import com.game.netty.sample.common.Constant;
import com.game.netty.sample.common.SampleLogicServer;
import com.game.netty.sample.common.SampleServer;

public class ServerStarter {

    public static void main(String[] args) throws Exception {
        SampleServer server = new SampleServer("directServer");
        server.start(Constant.PROXY_PORT);
    }

}
