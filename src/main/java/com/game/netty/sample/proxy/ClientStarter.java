package com.game.netty.sample.proxy;

import com.game.netty.sample.common.Constant;
import com.game.netty.sample.common.SampleClient;

public class ClientStarter {

    public static void main(String[] args) throws InterruptedException {
        SampleClient client = new SampleClient("client1");
        client.start(Constant.PROXY_PORT);
    }
}
