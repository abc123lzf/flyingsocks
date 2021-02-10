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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * @author lzf abc123lzf@126.com
 * @since 2020/12/16 7:08
 */
public class DirectForwardComponent extends AbstractComponent<ProxyComponent> implements ProxyRequestSubscriber {

    /**
     * Bootstrap模板
     */
    private final BootstrapTemplate connectBootstrapTemplate;

    /**
     * IO事件处理器
     */
    private final EventLoopGroup eventLoopGroup;


    public DirectForwardComponent(ProxyComponent component) {
        super("DirectForwardComponent", component);

        Bootstrap template = new Bootstrap();
        template.group(this.eventLoopGroup = parent.createNioEventLoopGroup(4))
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.AUTO_CLOSE, true);

        this.connectBootstrapTemplate = new BootstrapTemplate(template);
    }


    @Override
    protected void startInternal() {
        parent.registerSubscriber(this);
        super.startInternal();
    }

    @Override
    protected void stopInternal() {
        eventLoopGroup.shutdownGracefully();
    }

    @Override
    public void receive(ProxyRequest request) {
        final String host = request.getHost();
        final int port = request.getPort();

        log.trace("connect to server {}:{} established...", host, port);

        connectBootstrapTemplate.doConnect(host, port,
                ch -> ch.pipeline().addFirst(new ConnectHandler(request)),
                f -> {
                    if (!f.isSuccess()) {
                        handleConnectException(request, f.cause());
                        f.channel().close();
                        request.close();
                    }

                    request.addClientChannelCloseListener(_f -> f.channel().close());
                    log.trace("connect establish success, target server {}:{}", host, port);
                });
    }


    private void handleConnectException(ProxyRequest request, Throwable throwable) {
        if (throwable instanceof ConnectTimeoutException) {
            if (log.isInfoEnabled())
                log.info("connect to " + request.getHost() + ":" + request.getPort() + " failure, cause connect timeout");
        } else if (throwable instanceof UnknownHostException) {
            if (log.isWarnEnabled())
                log.warn("Connect failure: Unknow domain {}", request.getHost());
        } else {
            if (log.isWarnEnabled())
                log.warn("connect establish failure, from " + request.getHost() + ":" + request.getPort(), throwable);
        }

        request.closeClientChannel();
    }


    @Override
    public boolean receiveNeedlessProxy() {
        return true;
    }

    @Override
    public Set<ProxyRequest.Protocol> requestProtocol() {
        return ProxyRequestSubscriber.ONLY_TCP;
    }


    /**
     * 与目标服务器直连的进站处理器，一般用于无需代理的网站
     */
    private final class ConnectHandler extends ChannelInboundHandlerAdapter {
        final ProxyRequest request;

        private ConnectHandler(ProxyRequest request) {
            this.request = request;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws IOException {
            if (log.isTraceEnabled()) {
                log.trace("Channel Active from {}:{}", request.getHost(), request.getPort());
            }

            request.setClientMessageReceiver(new MessageReceiver() {
                @Override
                public void receive(ByteBuf message) {
                    ctx.channel().writeAndFlush(message);
                }

                @Override
                public void close() {
                    Channel channel = ctx.channel();
                    if (channel.isOpen()) {
                        channel.close();
                    }
                }
            });

            ctx.fireChannelActive();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof MessageDelivererCancelledException) {
                request.close();
                ctx.close();
                return;
            } else if (cause instanceof IOException) {
                log.trace("Direct TCP Connection force to close, from remote server {}:{}", request.getHost(), request.getPort());
                return;
            }

            log.warn("Exception caught in TCP ProxyHandler", cause);
            request.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.trace("connection close, from {}:{}", request.getHost(), request.getPort());

            ctx.pipeline().remove(this);
            request.close();

            ctx.fireChannelInactive();
            ctx.close();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof ByteBuf) {
                request.clientChannel().writeAndFlush(msg);
                return;
            }

            ctx.fireChannelRead(msg);
        }
    }

}
