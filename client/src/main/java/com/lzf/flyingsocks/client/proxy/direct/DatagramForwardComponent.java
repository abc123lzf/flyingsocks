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
package com.lzf.flyingsocks.client.proxy.direct;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.client.proxy.ProxyComponent;
import com.lzf.flyingsocks.client.proxy.ProxyRequest;
import com.lzf.flyingsocks.client.proxy.ProxyRequestSubscriber;
import com.lzf.flyingsocks.client.proxy.util.MessageDelivererCancelledException;
import com.lzf.flyingsocks.client.proxy.util.MessageReceiver;
import com.lzf.flyingsocks.util.BootstrapTemplate;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Set;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/14 6:19
 */
public class DatagramForwardComponent extends AbstractComponent<ProxyComponent> implements ProxyRequestSubscriber {

    private final BootstrapTemplate bootstrapTemplate;

    private final EventLoopGroup eventLoopGroup;

    public DatagramForwardComponent(ProxyComponent component) {
        super("DatagramForwardComponent", Objects.requireNonNull(component));
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(this.eventLoopGroup = parent.createNioEventLoopGroup(2))
                .channel(NioDatagramChannel.class);

        this.bootstrapTemplate = new BootstrapTemplate(bootstrap);
    }


    @Override
    protected void startInternal() {
        parent.registerSubscriber(this);
        super.startInternal();
    }

    @Override
    protected void stopInternal() {
        eventLoopGroup.shutdownGracefully();
        super.stopInternal();
    }

    @Override
    public void receive(ProxyRequest request) {
        bootstrapTemplate.doBind(0, channel -> {
            channel.pipeline().addLast(new ForwardHandler(request));
        }, future -> {
            if (!future.isSuccess()) {
                request.close();
            }
        });
    }

    @Override
    public boolean receiveNeedlessProxy() {
        return true;
    }

    @Override
    public boolean receiveNeedProxy() {
        return false;
    }

    @Override
    public Set<ProxyRequest.Protocol> requestProtocol() {
        return ProxyRequestSubscriber.ONLY_UDP;
    }


    private class ForwardHandler extends ChannelInboundHandlerAdapter {

        private final ProxyRequest proxyRequest;

        ForwardHandler(ProxyRequest proxyRequest) {
            this.proxyRequest = Objects.requireNonNull(proxyRequest);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            InetSocketAddress address = new InetSocketAddress(proxyRequest.getHost(), proxyRequest.getPort());

            proxyRequest.setClientMessageReceiver(new MessageReceiver() {
                @Override
                public void receive(ByteBuf message) {
                    DatagramPacket packet = new DatagramPacket(message, address);
                    ctx.writeAndFlush(packet, ctx.voidPromise());
                }

                @Override
                public void close() {
                    ctx.close();
                }
            });

            ctx.fireChannelActive();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof DatagramPacket) {
                DatagramPacket packet = (DatagramPacket) msg;
                proxyRequest.clientChannel().writeAndFlush(packet);
            } else {
                ctx.fireChannelRead(msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof MessageDelivererCancelledException) {
                ctx.close();
                return;
            }

            log.error("An error occur in ForwardHandler", cause);
        }
    }
}
