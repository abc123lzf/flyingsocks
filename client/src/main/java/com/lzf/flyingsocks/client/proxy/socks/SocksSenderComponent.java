package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.client.proxy.ProxyRequest;
import com.lzf.flyingsocks.client.proxy.ProxyRequestSubscriber;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Set;

public final class SocksSenderComponent extends AbstractComponent<SocksProxyComponent> {

    /**
     * TCP代理连接引导模板
     */
    private final Bootstrap connectBootstrap;

    /**
     * TCP代理订阅者
     */
    private TCPSocksProxyRequestSubscriber tcpRequsetSubscriber;


    SocksSenderComponent(SocksProxyComponent proxyComponent) {
        super("SocksSender", proxyComponent);
        connectBootstrap = new Bootstrap();
        connectBootstrap.group(parent.getWorkerEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.AUTO_CLOSE, true);
    }


    @Override
    protected void startInternal() {
        parent.registerSubscriber(this.tcpRequsetSubscriber = new TCPSocksProxyRequestSubscriber());
    }


    @Override
    protected void stopInternal() {
        parent.removeSubscriber(tcpRequsetSubscriber);
    }


    private final class TCPSocksProxyRequestSubscriber implements ProxyRequestSubscriber {
        @Override
        public void receive(final ProxyRequest req) {
            final SocksProxyRequest request = (SocksProxyRequest) req;
            final String host = request.getHost();
            final int port = request.getPort();

            if(log.isTraceEnabled())
                log.trace("connect to server {}:{} established...", host, port);

            Bootstrap b = connectBootstrap.clone();
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addFirst(new TCPConnectHandler(request));
                }
            });

            b.connect(host, port).addListener((ChannelFuture f) -> {
                if(!f.isSuccess()){
                    if(f.cause() instanceof ConnectTimeoutException) {
                        if(log.isInfoEnabled())
                            log.info("connect to " + request.getHost() + ":" + request.getPort() + " failure, cause connect timeout");
                    } else if(f.cause() instanceof UnknownHostException) {
                        if (log.isWarnEnabled())
                            log.warn("Connect failure: Unknow domain {}", request.getHost());
                    } else {
                        if (log.isWarnEnabled())
                            log.warn("connect establish failure, from " + request.getHost() + ":" + request.getPort(), f.cause());
                    }
                    request.clientChannel().close();
                    f.channel().close();
                } else {
                    if(log.isTraceEnabled())
                        log.trace("connect establish success, from {}:{}", request.getHost(), request.getPort());
                    request.setServerChannel(f.channel());
                }
            });
        }

        @Override
        public boolean receiveNeedProxy() {
            return false;
        }

        @Override
        public boolean receiveNeedlessProxy() {
            return true;
        }

        @Override
        public Class<? extends ProxyRequest> requestType() {
            return SocksProxyRequest.class;
        }

        @Override
        public Set<ProxyRequest.Protocol> requestProtcol() {
            return ONLY_TCP;
        }
    }


    /**
     * 与目标服务器直连的进站处理器，一般用于无需代理的网站
     */
    private final class TCPConnectHandler extends SimpleChannelInboundHandler<ByteBuf> {
        final SocksProxyRequest request;

        private TCPConnectHandler(SocksProxyRequest request) {
            super(false);
            this.request = request;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            if (log.isTraceEnabled())
                log.trace("Channel Active from {}:{}", request.getHost(), request.getPort());

            Channel sc = ctx.channel();
            request.setServerChannel(sc);
            ctx.fireChannelActive();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(cause instanceof IOException && log.isInfoEnabled()) {
                log.trace("Direct TCP Connection force to close, from remote server {}:{}", request.getHost(), request.getPort());
            } else if(log.isWarnEnabled()) {
                log.warn("Exception caught in TCP ProxyHandler", cause);
            }

            request.closeClientChannel();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if(log.isTraceEnabled()) {
                log.trace("connection close, from {}:{}", request.getHost(), request.getPort());
            }

            ctx.pipeline().remove(this);
            request.closeClientChannel();
            ctx.fireChannelInactive();
            ctx.close();
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            request.clientChannel().writeAndFlush(msg);
        }
    }
}
