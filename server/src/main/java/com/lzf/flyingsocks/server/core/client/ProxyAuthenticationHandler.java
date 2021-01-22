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

import com.lzf.flyingsocks.protocol.AuthMessage;
import com.lzf.flyingsocks.protocol.SerializationException;
import com.lzf.flyingsocks.server.core.ClientSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/15 7:47
 */
public class ProxyAuthenticationHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ProxyAuthenticationHandler.class);

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

    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws SerializationException {
        ClientSession session = ConnectionContext.clientSession(ctx.channel());
        Predicate<AuthMessage> authPredicate = ConnectionContext.authPredicate(ctx.channel());

        AuthMessage msg = new AuthMessage(buf);
        boolean auth = authPredicate.test(msg);
        if (!auth) {
            if (log.isTraceEnabled())
                log.trace("Auth failure, from client {}", ((SocketChannel) ctx.channel()).remoteAddress().getHostName());
            ctx.close();
            return;
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Auth success, from client {}", ((SocketChannel) ctx.channel()).remoteAddress().getHostName());
            }
        }

        session.passAuth();

        ChannelPipeline cp = ctx.pipeline();
        cp.remove(this).remove(DelimiterBasedFrameDecoder.class);

        byte[] delimiter = session.getDelimiterKey();
        cp.addLast(new DelimiterBasedFrameDecoder(1024 * 1000 * 50, Unpooled.wrappedBuffer(delimiter)));
        cp.addLast(new ProxyHandler());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof SerializationException) {
            log.info("Deserialize occur a exception", cause);
            ctx.close();
        }
    }
}
