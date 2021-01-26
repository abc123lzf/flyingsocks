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

import com.lzf.flyingsocks.protocol.Message;
import com.lzf.flyingsocks.protocol.PingMessage;
import com.lzf.flyingsocks.protocol.PongMessage;
import com.lzf.flyingsocks.protocol.SerializationException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/26 16:20
 */
@ChannelHandler.Sharable
public class HeartbeatMessageHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME = "HeartbeatMessageHandler";

    public static final HeartbeatMessageHandler INSTANCE = new HeartbeatMessageHandler();

    private HeartbeatMessageHandler() {
        super();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            int head = buf.getByte(buf.readerIndex());

            if (head == PingMessage.HEAD) {
                try {
                    PongMessage pong = new PongMessage();
                    ctx.writeAndFlush(pong, ctx.voidPromise());
                    return;
                } finally {
                    ReferenceCountUtil.release(msg);
                }
            } else if (head == PongMessage.HEAD) {
                try {
                    new PongMessage(buf);
                    return;
                } finally {
                    ReferenceCountUtil.release(msg);
                }
            }
        }

        ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof SerializationException) {
            SerializationException exception = (SerializationException) cause;
            Class<? extends Message> klass = exception.getMessageClass();
            if (klass == PingMessage.class || klass == PongMessage.class) {
                ctx.close();
                return;
            }
        }

        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            PingMessage ping = new PingMessage();
            ctx.writeAndFlush(ping);
            return;
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public boolean isSharable() {
        return true;
    }
}
