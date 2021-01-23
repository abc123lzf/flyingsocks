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
package com.lzf.flyingsocks.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.util.Arrays;
import java.util.Objects;

/**
 * 在发送消息时自动添加分隔符
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/23 23:14
 */
public class DelimiterOutboundHandler extends ChannelOutboundHandlerAdapter {

    /**
     * 分隔符ByteBuf
     */
    private final ByteBuf delimiter;


    public DelimiterOutboundHandler(byte[] delimiter) {
        Objects.requireNonNull(delimiter);
        byte[] arr = Arrays.copyOf(delimiter, delimiter.length);
        this.delimiter = Unpooled.wrappedBuffer(arr).asReadOnly();
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            delimiter.readerIndex(0);
            ctx.write(msg, promise);
            ctx.write(delimiter.retain(), promise);
        } else {
            ctx.write(msg, promise);
        }
    }
}
