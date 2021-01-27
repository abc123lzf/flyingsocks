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
import io.netty.buffer.CompositeByteBuf;

/**
 * 代理阶段消息
 * +-------+------+---------------+
 * |  SVID |  LEN |    MESSAGE    |
 * +-------+------+---------------+
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/27 4:06
 */
public abstract class ServiceStageMessage extends Message {

    public static final int LENGTH_FIELD_OFFSET = 1;

    public static final int LENGTH_FIELD_SIZE = 4;

    /**
     * 消息类型
     */
    private byte serviceId;


    protected ServiceStageMessage(byte serviceId) {
        super();
        this.serviceId = serviceId;
    }


    protected ServiceStageMessage(ByteBuf buf) throws SerializationException {
        super(buf);
    }


    @Override
    public final ByteBuf serialize(ByteBufAllocator allocator) throws SerializationException {
        ByteBuf header = allocator.directBuffer(5);
        header.writeByte(serviceId);

        ByteBuf body = serialize0(allocator);
        int length = body.readableBytes();
        header.writeInt(length);

        CompositeByteBuf buf = allocator.compositeBuffer(2);
        buf.addComponent(true, header);
        buf.addComponent(true, body);
        return buf;
    }

    /**
     * 序列化消息体(MESSAGE部分)
     */
    protected abstract ByteBuf serialize0(ByteBufAllocator allocator) throws SerializationException;


    @Override
    protected final void deserialize(ByteBuf buf) throws SerializationException {
        this.serviceId = buf.readByte();

        int length = buf.readInt();
        if (buf.readableBytes() != length) {
            throw new SerializationException(getClass(), "Length field is NOT equals to buf's readable bytes");
        }

        deserialize0(buf);
    }

    /**
     * 反序列化消息体(MESSAGE部分)
     */
    protected abstract void deserialize0(ByteBuf buf) throws SerializationException;

    /**
     * @return 消息类型ID
     */
    public final byte getServiceId() {
        return serviceId;
    }
}
