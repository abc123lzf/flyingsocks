package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Arrays;

public class DelimiterMessage implements Message {
    /**
     * 分隔符字节数，建议16字节以上避免与报文数据混淆
     */
    public static final int DEFAULT_SIZE = 16;

    private byte[] delimiter;

    public DelimiterMessage(byte[] delimiter) {
        setDelimiter(delimiter);
    }

    public DelimiterMessage(ByteBuf serialBuf) throws SerializationException {
        deserialize(serialBuf);
    }

    @Override
    public ByteBuf serialize() throws SerializationException {
        return Unpooled.buffer(DEFAULT_SIZE).writeBytes(delimiter);
    }

    @Override
    public void deserialize(ByteBuf buf) throws SerializationException {
        int size = buf.readableBytes();
        if(size < 16)
            throw new SerializationException("Delimiter length must be " + DEFAULT_SIZE + " bytes");

        try {
            byte[] b = new byte[DEFAULT_SIZE];
            buf.readBytes(b);
            this.delimiter = b;
        } catch (IndexOutOfBoundsException e) {
            throw new SerializationException("Delimiter Message error", e);
        }
    }

    public ByteBuf getDelimiter() {
        return Unpooled.buffer(DEFAULT_SIZE).writeBytes(delimiter);
    }

    public void setDelimiter(byte[] delimiter) {
        if(delimiter.length != DEFAULT_SIZE)
            throw new IllegalArgumentException("Delimiter length must be " + DEFAULT_SIZE + " bytes");
        this.delimiter = Arrays.copyOf(delimiter, delimiter.length);
    }

    public void setDelimiter(ByteBuf buf) {
        if(buf.readableBytes() < DEFAULT_SIZE)
            throw new IllegalArgumentException("Delimiter length must be " + DEFAULT_SIZE + " bytes");
        byte[] b = new byte[DEFAULT_SIZE];
        buf.readBytes(b);
        this.delimiter = b;
    }
}
