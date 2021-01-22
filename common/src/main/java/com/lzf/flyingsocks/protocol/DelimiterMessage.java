/*
 * Copyright (c) 2019 abc123lzf <abc123lzf@126.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

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
    public ByteBuf serialize(ByteBufAllocator allocator) throws SerializationException {
        try {
            ByteBuf buf = allocator.buffer(LENGTH);
            buf.writeBytes(MAGIC);
            buf.writeBytes(delimiter);
            return buf;
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void deserialize(ByteBuf buf) throws SerializationException {
        int size = buf.readableBytes();
        if (size < LENGTH) {
            throw new SerializationException("Delimiter Message length must be " + LENGTH + " bytes: including " +
                    MAGIC.length + " bytes magic and " + DEFAULT_SIZE + " bytes delimiter content");
        }

        byte[] magic = new byte[MAGIC.length];
        buf.readBytes(magic);
        if (!Arrays.equals(magic, MAGIC)) {
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

    public byte[] getDelimiter() {
        byte[] delimiter = this.delimiter;
        if (delimiter == null || delimiter.length == 0) {
            return null;
        }

        return Arrays.copyOf(delimiter, delimiter.length);
    }

    public void setDelimiter(byte[] delimiter) {
        if (delimiter.length != DEFAULT_SIZE) {
            throw new IllegalArgumentException("Delimiter length must be " + DEFAULT_SIZE + " bytes");
        }
        this.delimiter = Arrays.copyOf(delimiter, delimiter.length);
    }

    public void setDelimiter(ByteBuf buf) {
        if (buf.readableBytes() < DEFAULT_SIZE) {
            throw new IllegalArgumentException("Delimiter length must be " + DEFAULT_SIZE + " bytes");
        }

        byte[] b = new byte[DEFAULT_SIZE];
        buf.readBytes(b);
        this.delimiter = b;
    }
}
