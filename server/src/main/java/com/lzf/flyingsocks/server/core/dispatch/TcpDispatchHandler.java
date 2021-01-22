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
package com.lzf.flyingsocks.server.core.dispatch;

import com.lzf.flyingsocks.protocol.ProxyResponseMessage;
import com.lzf.flyingsocks.server.core.ProxyTask;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/19 20:00
 */
class TcpDispatchHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(TcpDispatchHandler.class);

    private final ProxyTask proxyTask;

    TcpDispatchHandler(ProxyTask task) {
        this.proxyTask = task;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            log.info("Target remote host {}:{} close, cause IOException", host(), port());
        } else {
            log.warn("DispathcerHandelr occur a exception", cause);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.trace("Connection close by {}:{}", host(), port());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ByteBuf buf = proxyTask.getRequestMessage().getMessage();
        ctx.writeAndFlush(buf);
        ctx.fireChannelActive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            try {
                channelRead0(ctx, (ByteBuf) msg);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        log.trace("Receive from {}:{} response.", host(), port());

        ProxyResponseMessage prm = new ProxyResponseMessage(proxyTask.getRequestMessage().serialId());
        prm.setState(ProxyResponseMessage.State.SUCCESS);
        prm.setMessage(msg.retain());
        try {
            proxyTask.session().writeAndFlushMessage(prm.serialize(ctx.alloc()));
        } catch (IllegalStateException e) {
            msg.release();
            ctx.close();
        }
    }


    private int port() {
        return proxyTask.getRequestMessage().getPort();
    }


    private String host() {
        return proxyTask.getRequestMessage().getHost();
    }
}
