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
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/19 19:55
 */
class UdpDispatchHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(UdpDispatchHandler.class);

    private final ProxyTask proxyTask;

    private final String host;

    private final int port;

    UdpDispatchHandler(ProxyTask task) {
        this.proxyTask = task;
        this.host = proxyTask.getRequestMessage().getHost();
        this.port = proxyTask.getRequestMessage().getPort();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf buf = proxyTask.getRequestMessage().getMessage();
        DatagramPacket packet = new DatagramPacket(buf, new InetSocketAddress(host, port));
        ctx.writeAndFlush(packet);
        super.channelActive(ctx);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DatagramPacket) {
            try {
                channelRead0(ctx, (DatagramPacket) msg);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        log.trace("Receive from {}:{} Datagram.", host, port);

        ProxyResponseMessage prm = new ProxyResponseMessage(proxyTask.getRequestMessage().serialId());
        prm.setState(ProxyResponseMessage.State.SUCCESS);
        prm.setMessage(msg.content().retain());
        try {
            proxyTask.session().writeAndFlushMessage(prm.serialize(ctx.alloc()));
        } catch (IllegalStateException e) {
            ctx.close();
        }
    }

}
