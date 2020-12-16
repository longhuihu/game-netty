package com.game.netty.util;

public class StringTool {
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    private StringTool() {
    }
}
