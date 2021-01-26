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
public class CertResponseMessage extends Message {

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
        if (update) {
            this.file = Objects.requireNonNull(file);
            this.length = file.length;
        }
    }

    public CertResponseMessage(ByteBuf buf) throws SerializationException {
        super(buf);
    }

    @Override
    public ByteBuf serialize(ByteBufAllocator allocator) {
        ByteBuf buf;
        if (update) {
            buf = allocator.buffer(4 + file.length + END_MARK.length);
        } else {
            buf = allocator.buffer(4 + END_MARK.length);
        }

        if (update) {
            buf.writeInt((1 << 31) | length);
            buf.writeBytes(file);
        } else {
            buf.writeInt(0);
        }

        buf.writeBytes(END_MARK); //写入分隔符

        return buf;
    }

    @Override
    protected void deserialize(ByteBuf buf) throws SerializationException {
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
