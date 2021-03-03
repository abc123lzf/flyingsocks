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
package com.lzf.flyingsocks.misc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 用于检测消息头是否合法，辨别出非flyingsocks端并强制关闭连接
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/26 4:09
 */
@ChannelHandler.Sharable
public final class MessageHeaderCheckHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(MessageHeaderCheckHandler.class);

    private final byte[] header;

    private final int headerLength;

    public MessageHeaderCheckHandler(byte[] header) {
        this.header = Objects.requireNonNull(header);
        this.headerLength = header.length;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;

            if (buf.readableBytes() < headerLength) {
                ReferenceCountUtil.release(msg);
                ctx.close();
                return;
            }

            int index = buf.readerIndex();
            boolean pass = true;
            for (int i = 0; i < headerLength; i++) {
                if (buf.getByte(index + i) != header[i]) {
                    pass = false;
                }
            }

            if (!pass) {
                log.warn("Message header check failure");
                ReferenceCountUtil.release(msg);
                ctx.close();
                return;
            }
        }

        ctx.fireChannelRead(msg);
    }


    @Override
    public boolean isSharable() {
        return true;
    }
}
