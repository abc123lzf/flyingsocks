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
 * 证书请求报文
 * +---------+---------+-------------------------+
 * |Auth Type| Length  |           Auth          |
 * | (1 byte)|(2 bytes)|         Message         |
 * +---------+---------+-------------+-----------+
 * |           Cert File MD5         |  END_MARK |
 * |             (16 bytes)          | 0x00FF00FF|
 * +---------------------------------+-----------+
 */
public class CertRequestMessage extends AuthMessage implements Message {

    public static final byte[] END_MARK;

    static {
        END_MARK = new byte[4];
        END_MARK[0] = (byte) 0x00;
        END_MARK[1] = (byte) 0xFF;
        END_MARK[2] = (byte) 0x00;
        END_MARK[3] = (byte) 0xFF;
    }

    private byte[] certMD5;

    public CertRequestMessage(AuthMethod method, byte[] certMD5) {
        super(method);
        this.certMD5 = certMD5 != null ? Arrays.copyOf(certMD5, certMD5.length) : new byte[16];
    }

    public CertRequestMessage(ByteBuf buf) throws SerializationException {
        deserialize(buf);
    }

    @Override
    public ByteBuf serialize(ByteBufAllocator allocator) throws SerializationException {
        ByteBuf buf = super.serialize(allocator);
        try {
            ByteBuf res = allocator.buffer(buf.readableBytes() + 16 + 4);
            res.writeBytes(buf);
            res.writeBytes(certMD5);
            res.writeBytes(END_MARK);
            return res;
        } finally {
            buf.release();
        }
    }

    @Override
    public void deserialize(ByteBuf buf) throws SerializationException {
        super.deserialize(buf);
        if (buf.readableBytes() != 16) {
            throw new SerializationException("MD5 Infomation must be 16 bytes long");
        }

        byte[] b = new byte[16];
        buf.readBytes(b);
        this.certMD5 = b;
    }

    public byte[] getCertMD5() {
        return certMD5;
    }

    public void setCertMD5(byte[] certMD5) {
        this.certMD5 = certMD5;
    }
}
