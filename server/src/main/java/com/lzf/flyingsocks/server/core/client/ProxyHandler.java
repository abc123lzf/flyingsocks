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
package com.lzf.flyingsocks.server.core.client;

import com.lzf.flyingsocks.protocol.PingMessage;
import com.lzf.flyingsocks.protocol.PongMessage;
import com.lzf.flyingsocks.protocol.ProxyRequestMessage;
import com.lzf.flyingsocks.protocol.SerializationException;
import com.lzf.flyingsocks.server.core.ClientSession;
import com.lzf.flyingsocks.server.core.ProxyTask;
import com.lzf.flyingsocks.server.core.ProxyTaskManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/15 8:02
 */
class ProxyHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ProxyHandler.class);

    private ClientSession clientSession;

    private ProxyTaskManager proxyTaskManager;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        ClientSession session = ConnectionContext.clientSession(ctx.channel());
        ProxyTaskManager manager = ConnectionContext.proxyTaskManager(ctx.channel());
        if (session == null || manager == null) {
            throw new IllegalStateException();
        }

        this.clientSession = session;
        this.proxyTaskManager = manager;

        ctx.pipeline().addFirst(new IdleStateHandler(20, 0, 0));
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            PingMessage ping = new PingMessage();
            ctx.writeAndFlush(ping, ctx.voidPromise());
            return;
        }

        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            try {
                byte head = buf.getByte(0);
                if (head == ProxyRequestMessage.HEAD) {
                    channelRead0(ctx, buf);
                } else if (head == PingMessage.HEAD) {
                    PingMessage ping = new PingMessage();
                    ping.serialize(ctx.alloc());
                    PongMessage pong = new PongMessage();
                    ctx.writeAndFlush(pong);
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }


    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws SerializationException {
        ProxyRequestMessage msg = new ProxyRequestMessage(buf);
        ProxyTask task = new ProxyTask(msg, clientSession);
        proxyTaskManager.publish(task);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof SerializationException) {
            log.warn("Serialize request occur a exception", cause);
        }

        ctx.close();
    }
}
