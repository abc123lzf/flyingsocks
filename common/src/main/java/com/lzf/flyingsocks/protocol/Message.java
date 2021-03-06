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

import java.util.Objects;

/**
 * 统一报文接口，用于客户端与服务端之间的通信
 */
public abstract class Message {

    /**
     * 用于发送方构建消息
     */
    protected Message() {
        super();
    }

    /**
     * 用于接收方实例化消息
     *
     * @param src 完整消息数据
     * @throws SerializationException 反序列化发生异常
     */
    protected Message(ByteBuf src) throws SerializationException {
        try {
            Objects.requireNonNull(src, "ByteBuf source is null!");
            deserialize(src);
        } catch (RuntimeException e) {
            throw new SerializationException(getClass(), e);
        }
    }


    /**
     * 将报文对象序列化为{@link ByteBuf}
     *
     * @param allocator {@link ByteBufAllocator}实例
     * @return 序列化后的 {@link ByteBuf}
     * @throws SerializationException 序列化异常
     */
    public abstract ByteBuf serialize(ByteBufAllocator allocator) throws SerializationException;


    /**
     * 反序列化逻辑
     *
     * @param buf Netty的{@link ByteBuf}对象
     * @throws SerializationException 反序列化异常
     */
    protected abstract void deserialize(ByteBuf buf) throws SerializationException;
}
