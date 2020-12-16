package com.game.netty.sample.common;

public class LogUtil {

    public static void print(String tag, String content) {
        System.out.println(String.format("[%.3f] [%s] %s", System.currentTimeMillis()*1.0 / 1000, tag, content));
    }

    public static void print() {
        System.out.println();
    }
}
