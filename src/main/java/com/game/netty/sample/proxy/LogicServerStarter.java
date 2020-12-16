package com.game.netty.sample.proxy;

import com.game.netty.sample.common.Constant;
import com.game.netty.sample.common.SampleLogicServer;

public class LogicServerStarter {

    public static void main(String[] args) throws InterruptedException {
        SampleLogicServer logicServer1 = new SampleLogicServer("logicServer1");
        logicServer1.start(1, Constant.LOGIC_SERVER_1_PORT);

        SampleLogicServer logicServer2 = new SampleLogicServer("logicServer2");
        logicServer2.start(1, Constant.LOGIC_SERVER_2_PORT);
    }

}
