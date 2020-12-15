package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import java.util.Arrays;

/**
 * 分隔符握手报文
 * +-------+---------------------------------------+
 * | Magic |              Delimiter                |
 * |6 Bytes|          128 Bit / 16 bytes           |
 * +-------+---------------------------------------+
 * <p>
 * 6字节魔数+16字节分隔符
 */
public class DelimiterMessage implements Message {

    /**
     * 客户端和服务端共同约定的魔数,用于鉴别非flyingsocks客户端的访问
     */
    public static final byte[] MAGIC = new byte[]{(byte) 0xE4, (byte) 0xBC, (byte) 0x8A,
            (byte) 0xE8, (byte) 0x94, (byte) 0x93};

    /**
     * 分隔符字节数，建议16字节以上避免与报文数据混淆
     */
    public static final int DEFAULT_SIZE = 16;

    /**
     * 消息长度
     */
    public static final int LENGTH = MAGIC.length + DEFAULT_SIZE;

    /**
     * 分隔符内容
     */
    private byte[] delimiter;

    public DelimiterMessage(byte[] delimiter) {
        setDelimiter(delimiter);
    }

    public DelimiterMessage(ByteBuf serialBuf) throws SerializationException {
        deserialize(serialBuf);
    }

    @Override
    public ByteBuf serialize() throws SerializationException {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer(LENGTH);
        buf.writeBytes(MAGIC);
        buf.writeBytes(delimiter);
        try {
            return buf;
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void deserialize(ByteBuf buf) throws SerializationException {
        buf = buf.copy();
        int size = buf.readableBytes();
        if (size < LENGTH) {
            buf.release();
            throw new SerializationException("Delimiter Message length must be " + LENGTH + " bytes: including " +
                    MAGIC.length + " bytes magic and " + DEFAULT_SIZE + " bytes delimiter content");
        }

        byte[] magic = new byte[MAGIC.length];
        buf.readBytes(magic);
        if (!Arrays.equals(magic, MAGIC)) {
            buf.release();  //防止非flyingsocks客户端访问导致此处内存泄漏
            throw new SerializationException("Illegal magic number: " + Arrays.toString(magic));
        }

        try {
            byte[] b = new byte[DEFAULT_SIZE];
            buf.readBytes(b);
            this.delimiter = b;
        } catch (IndexOutOfBoundsException e) {
            throw new SerializationException("Delimiter Message error", e);
        }
    }

    public ByteBuf getDelimiter() {
        if (delimiter == null || delimiter.length == 0) {
            return null;
        }

        return PooledByteBufAllocator.DEFAULT.buffer(DEFAULT_SIZE).writeBytes(delimiter);
    }

    public void setDelimiter(byte[] delimiter) {
        if (delimiter.length != DEFAULT_SIZE)
            throw new IllegalArgumentException("Delimiter length must be " + DEFAULT_SIZE + " bytes");
        this.delimiter = Arrays.copyOf(delimiter, delimiter.length);
    }

    public void setDelimiter(ByteBuf buf) {
        if (buf.readableBytes() < DEFAULT_SIZE)
            throw new IllegalArgumentException("Delimiter length must be " + DEFAULT_SIZE + " bytes");
        byte[] b = new byte[DEFAULT_SIZE];
        buf.readBytes(b);
        this.delimiter = b;
    }
}
