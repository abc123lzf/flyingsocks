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
package com.lzf.flyingsocks.client.proxy.transparent;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.client.proxy.ProxyComponent;
import com.lzf.flyingsocks.client.proxy.ProxyRequest;
import com.lzf.flyingsocks.client.proxy.misc.MessageDelivererCancelledException;
import com.lzf.flyingsocks.misc.BaseUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * 实现Linux环境下的透明代理，需要使用epoll
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/24 18:55
 */
public class LinuxTransparentProxyComponent extends AbstractComponent<ProxyComponent> {

    private final EpollEventLoopGroup eventLoopGroup;

    private int bindPort;

    {
        if (!Epoll.isAvailable()) {
            throw new ComponentException("Epoll not support", Epoll.unavailabilityCause());
        }

        try {
            Class.forName(LinuxNative.class.getName());
        } catch (ClassNotFoundException | UnsatisfiedLinkError e) {
            throw new ComponentException("Could not load Transparent Native Library", e);
        }
    }

    public LinuxTransparentProxyComponent(ProxyComponent component) {
        super("LinuxTransparentProxyComponent", Objects.requireNonNull(component));
        int cpus = getConfigManager().availableProcessors();
        this.eventLoopGroup = new EpollEventLoopGroup(Math.max(cpus / 2, 1));
    }

    @Override
    protected void initInternal() {
        TransparentProxyConfig config = getConfigManager().getConfig(TransparentProxyConfig.NAME, TransparentProxyConfig.class);
        if (config == null) {
            throw new ComponentException("Config [TransparentProxyConfig] not register");
        }

        int port = config.getBindPort();
        if (!BaseUtils.isPort(port)) {
            throw new ComponentException("Port: " + port);
        }

        this.bindPort = port;

        super.initInternal();
    }

    @Override
    protected void startInternal() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(EpollServerSocketChannel.class)
                .option(EpollChannelOption.IP_TRANSPARENT, true)
                .childHandler(new ChannelInitializer<EpollSocketChannel>() {
                    @Override
                    protected void initChannel(EpollSocketChannel ch) {
                        InetSocketAddress address = LinuxNative.getTargetAddress(ch);
                        if (address == null) {
                            log.warn("Could not get target address, client address: {}", ch.remoteAddress());
                            ch.close();
                            return;
                        }

                        log.debug("Proxy target address: {}, from client: {}", address, ch.remoteAddress());
                        ChannelPipeline pipeline = ch.pipeline();

                        ProxyRequest request = new ProxyRequest(address.getHostName(), address.getPort(), ch, ProxyRequest.Protocol.TCP);
                        pipeline.addLast(new IdleStateHandler(0, 20, 0));
                        pipeline.addLast(new ProxyHandler(request));
                    }
                });

        int port = this.bindPort;
        ChannelFuture future = bootstrap.bind("0.0.0.0", port).awaitUninterruptibly();
        if (!future.isSuccess()) {
            log.error("Bind port {} failure: {}", port, future.cause().getMessage());
            throw new ComponentException("Bind port failure", future.cause());
        }

        super.startInternal();
    }

    @Override
    protected void stopInternal() {
        eventLoopGroup.shutdownGracefully();
        super.stopInternal();
    }


    private class ProxyHandler extends ChannelInboundHandlerAdapter {

        private final ProxyRequest proxyRequest;

        ProxyHandler(ProxyRequest request) {
            this.proxyRequest = Objects.requireNonNull(request);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                if (proxyRequest.isClose()) {
                    ctx.close();
                }
                return;
            }
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof MessageDelivererCancelledException) {
                ctx.close();
                return;
            } else if (cause instanceof IOException) {
                log.info("Remote client [{}] force to close: {}", ctx.channel().remoteAddress(), cause.getMessage());
                return;
            }

            log.error("An error occur in ProxyHandler", cause);
            ctx.close();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof ByteBuf) {
                try {
                    proxyRequest.transferClientMessage((ByteBuf) msg);
                } finally {
                    ReferenceCountUtil.release(msg);
                }
            }

            ctx.fireChannelRead(msg);
        }
    }
}
