package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * 证书响应报文
 * 0 1                        4                                  Length + 4
 * +-+------------------------+----------------------------------+
 * |U|        Length          |              File                |
 * |P|        31 bit          |                                  |
 * +-+------------------------+----------------------------------+
 */
public class CertResponseMessage implements Message {

    public static final byte[] END_MARK = CertRequestMessage.END_MARK;

    /**
     * 是否需要更新证书
     */
    private boolean update;

    /**
     * 证书长度
     */
    private int length;

    /**
     * 证书文件
     */
    private byte[] file;

    public CertResponseMessage(boolean update, byte[] file) {
        this.update = update;
        if(update) {
            this.file = Objects.requireNonNull(file);
            this.length = file.length;
        }
    }

    public CertResponseMessage(ByteBuf buf) throws SerializationException {
        deserialize(buf);
    }

    @Override
    public ByteBuf serialize() {
        ByteBuf buf;
        if(update)
            buf = Unpooled.buffer(4 + file.length + END_MARK.length);
        else
            buf = Unpooled.buffer(4 + END_MARK.length);

        if(update) {
            buf.writeInt((1 << 31) | length);
            buf.writeBytes(file);
        } else {
            buf.writeInt(0);
        }

        buf.writeBytes(END_MARK); //写入分隔符

        return buf;
    }

    @Override
    public void deserialize(ByteBuf buf) throws SerializationException {
        try {
            int val = buf.readInt();
            this.update = val < 0; //判断首位是否为1
            if (update) {
                int length = val ^ (1 << 31);
                byte[] b = new byte[length];
                this.length = length;
                buf.readBytes(b);
                this.file = b;
            }
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    public boolean needUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public int getLength() {
        return length;
    }

    public InputStream getFile() {
        return new ByteArrayInputStream(file);
    }

    public void setFile(byte[] file) {
        this.file = file;
    }
}
