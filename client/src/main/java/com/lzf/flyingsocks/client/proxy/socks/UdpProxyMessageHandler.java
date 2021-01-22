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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/14 4:04
 */
public class UdpProxyMessageHandler extends ChannelInboundHandlerAdapter {

    private final ProxyRequestManager proxyRequestManager;

    private final Map<InetSocketAddress, ProxyRequest> requestCache = new HashMap<>();

    public UdpProxyMessageHandler(ProxyRequestManager proxyRequestManager) {
        this.proxyRequestManager = Objects.requireNonNull(proxyRequestManager);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof UdpProxyMessage) {
            try {
                channelRead0(ctx, (UdpProxyMessage) msg);
            } finally {
                ((UdpProxyMessage) msg).release();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }


    private void channelRead0(ChannelHandlerContext ctx, UdpProxyMessage message) throws IOException {
        String host = message.getHost();
        int port = message.getPort();
        ByteBuf data = message.getData().retain();

        InetSocketAddress address = new InetSocketAddress(host, port);
        ProxyRequest request = requestCache.get(address);
        if (request != null) {
            if (!request.isClose()) {
                request.transferClientMessage(data);
                return;
            }

            requestCache.remove(address);
        }

        request = new ProxyRequest(host, port, ctx.channel(), ProxyRequest.Protocol.UDP);
        requestCache.put(address, request);
        request.transferClientMessage(data);
        proxyRequestManager.publish(request);

        cleanClosedRequest();
    }


    private void cleanClosedRequest() {
        requestCache.values().removeIf(ProxyRequest::isClose);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        requestCache.values().forEach(ProxyRequest::close);
        requestCache.clear();
    }
}
