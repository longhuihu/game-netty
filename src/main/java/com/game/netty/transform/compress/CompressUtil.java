package com.game.netty.transform.compress;

import io.netty.buffer.ByteBuf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * longhuihu
 */
public class CompressUtil {

    /**
     * 对byteBuf的可读数据执行压缩（解压缩），并将结果写入byteBuf，调用者必须确保byteBuf可写，容量可扩展
     *
     * @param byteBuf
     * @param compress
     */
    public static void handleCompress(ByteBuf byteBuf, boolean compress) throws IOException, DataFormatException {
        byte[] buf;
        int offset = 0;
        int length = byteBuf.readableBytes();
        int readIndex = byteBuf.readerIndex();
        if (!byteBuf.hasArray()) {
            buf = new byte[length];
            byteBuf.getBytes(readIndex, buf);
        } else {
            buf = byteBuf.array();
            offset = byteBuf.arrayOffset() + readIndex;
        }

        byte[] data;
        if (compress) {
            data = CompressUtil.compress(buf, offset, length);
        } else {
            data = CompressUtil.uncompress(buf, offset, length);
        }

        byteBuf.writerIndex(readIndex);
        byteBuf.ensureWritable(data.length);
        byteBuf.writeBytes(data);
    }

    private static byte[] compress(byte[] data, int offset, int length) throws IOException {
        byte[] output;

        Deflater compressor = new Deflater();

        compressor.reset();
        compressor.setLevel(Deflater.BEST_SPEED);
        compressor.setInput(data, offset, length);
        compressor.finish();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length)) {
            byte[] buf = new byte[1024];
            while (!compressor.finished()) {
                int i = compressor.deflate(buf);
                bos.write(buf, 0, i);
            }
            output = bos.toByteArray();
        }
        compressor.end();
        return output;
    }

    //  解压缩
    private static byte[] uncompress(byte[] input, int offset, int length) throws IOException, DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(input, offset, length);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length)) {
            byte[] buff = new byte[64];
            while (!inflater.finished()) {
                int count = inflater.inflate(buff);
                baos.write(buff, 0, count);
            }
            inflater.end();
            return baos.toByteArray();
        }
    }

    private CompressUtil() {

    }
}
