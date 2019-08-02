package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

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
        ByteBuf buf = super.serialize();
        ByteBuf md5Buf = Unpooled.buffer(16 + 4);
        md5Buf.writeBytes(certMD5);
        md5Buf.writeBytes(END_MARK);

        CompositeByteBuf cbf = Unpooled.compositeBuffer();
        cbf.addComponent(true, buf);
        cbf.addComponent(true, md5Buf);
        return cbf;
    }

    @Override
    public void deserialize(ByteBuf buf) throws SerializationException {
        super.deserialize(buf);
        if(buf.readableBytes() != 16)
            throw new SerializationException("MD5 Infomation must be 16 bytes long");
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
