package com.game.netty.sample.proxy;

import com.game.netty.ServerDefine;
import com.game.netty.sample.common.Constant;
import com.game.netty.sample.common.SampleProxy;

import java.util.ArrayList;
import java.util.List;

public class ProxyStarter {
    public static void main(String[] args) throws Exception {

        List<ServerDefine> logicServers = new ArrayList<>();
        logicServers.add(new ServerDefine(1, "127.0.0.1", Constant.LOGIC_SERVER_1_PORT));
        logicServers.add(new ServerDefine(2, "127.0.0.1", Constant.LOGIC_SERVER_2_PORT));

        SampleProxy proxy = new SampleProxy("proxy");
        proxy.start(Constant.PROXY_PORT, logicServers);
    }
}
