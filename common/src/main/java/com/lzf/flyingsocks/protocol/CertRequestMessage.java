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
    public ByteBuf serialize() throws SerializationException {
        ByteBufAllocator allocator = getAllocator();

        ByteBuf buf = super.serialize();

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
