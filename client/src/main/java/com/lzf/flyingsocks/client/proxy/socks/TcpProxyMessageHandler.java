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
package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.client.proxy.ProxyRequest;
import com.lzf.flyingsocks.client.proxy.ProxyRequestManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/12 5:58
 */
public class TcpProxyMessageHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(TcpProxyMessageHandler.class);

    private final ProxyRequest proxyRequest;

    public TcpProxyMessageHandler(ProxyRequest proxyRequest, ProxyRequestManager manager) {
        this.proxyRequest = Objects.requireNonNull(proxyRequest);
        manager.publish(proxyRequest);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            try {
                proxyRequest.transferClientMessage(buf);
            } finally {
                buf.release();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            log.info("Local client close connection, from {}", ctx.channel().remoteAddress());
            ctx.close();
        } else if (log.isWarnEnabled()) {
            log.warn("Exception caught in TCPProxyMessageHandler", cause);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        proxyRequest.close();
        ctx.pipeline().remove(this);
        ctx.fireChannelInactive();
    }
}
