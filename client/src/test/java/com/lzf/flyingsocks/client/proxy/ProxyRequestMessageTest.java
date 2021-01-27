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
package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.protocol.ProxyRequestMessage;
import com.lzf.flyingsocks.protocol.SerializationException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/27 15:44
 */
public class ProxyRequestMessageTest {

    public static void main(String[] args) throws SerializationException {
        ByteBufAllocator allocator = UnpooledByteBufAllocator.DEFAULT;

        ProxyRequestMessage message = new ProxyRequestMessage(0, ProxyRequestMessage.Protocol.TCP);
        message.setHost("www.baidu.com");
        message.setPort(443);
        message.setMessage(Unpooled.wrappedBuffer(new byte[] {1, 2, 3, 4, 5}));

        ByteBuf buf = message.serialize(allocator);
        System.out.println(buf.readableBytes());

        ProxyRequestMessage res = new ProxyRequestMessage(buf);
        System.out.println(res.toString());
        System.out.println(res.getMessage().readableBytes());
    }

}
