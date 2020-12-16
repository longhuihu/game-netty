package com.game.netty.sample.common;

import com.game.netty.client.codec.MessageBodyCodec;
import io.netty.buffer.ByteBuf;

public class StringCodec implements MessageBodyCodec<String>  {
    @Override
    public int bodyEncodeSize(String message) {
        return message.getBytes().length;
    }

    @Override
    public void encodeBody(String body, ByteBuf out) {
        out.writeBytes(body.getBytes());
    }

    @Override
    public String decodeMessageBody(long messageHead, ByteBuf bodyBuf, int length) {
        byte[] data = new byte[length];
        bodyBuf.readBytes(data);
        return new String(data);
    }
}
