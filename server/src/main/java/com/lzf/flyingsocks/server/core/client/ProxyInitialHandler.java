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

import com.lzf.flyingsocks.protocol.DelimiterMessage;
import com.lzf.flyingsocks.protocol.SerializationException;
import com.lzf.flyingsocks.server.core.ClientSession;
import com.lzf.flyingsocks.util.DelimiterOutboundHandler;
import com.lzf.flyingsocks.util.FSMessageChannelOutboundHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * 用于初始化代理请求连接
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/17 13:19
 */
class ProxyInitialHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ProxyInitialHandler.class);

    private static final int MAX_AUTH_FRAME_SIZE = 1024 * 10;

    private ClientSession clientSession;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        this.clientSession = ConnectionContext.clientSession(channel);
        try {
            assert ConnectionContext.authPredicate(channel) != null;
            assert ConnectionContext.proxyTaskManager(channel) != null;
            assert this.clientSession != null;
        } catch (AssertionError error) {
            throw new IllegalStateException(error);
        }
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


    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws SerializationException {
        DelimiterMessage message = new DelimiterMessage(msg);
        byte[] delimiter = message.getDelimiter();
        clientSession.setDelimiterKey(delimiter);

        ChannelPipeline cp = ctx.pipeline();
        removeHandler(cp);
        ctx.write(new DelimiterMessage(delimiter).serialize(ctx.alloc()), ctx.voidPromise());
        addOutboundHandler(cp, delimiter);
        addInboundHandler(cp, delimiter);
        ctx.flush();
    }


    private void removeHandler(ChannelPipeline pipeline) {
        pipeline.remove(this);
        pipeline.remove(FixedLengthFrameDecoder.class);
    }

    private void addOutboundHandler(ChannelPipeline pipeline, byte[] delimiter) {
        pipeline.addLast(new DelimiterOutboundHandler(delimiter));
        pipeline.addLast(FSMessageChannelOutboundHandler.INSTANCE);
    }

    private void addInboundHandler(ChannelPipeline pipeline, byte[] delimiter) {
        pipeline.addLast(new DelimiterBasedFrameDecoder(MAX_AUTH_FRAME_SIZE,
                Unpooled.wrappedBuffer(Arrays.copyOf(delimiter, delimiter.length))));
        pipeline.addLast(new ProxyAuthenticationHandler());
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof SerializationException) {
            ctx.close();
        }

        log.error("An exception occur in ProxyInitialHandler", cause);
        ctx.close();
    }

}
