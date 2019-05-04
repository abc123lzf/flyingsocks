package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socks.*;

public final class SocksReceiverComponent extends AbstractComponent<SocksProxyComponent> {

    private ServerBootstrap serverBootstrap;

    private EventLoopGroup socksReceiveGroup;

    public SocksReceiverComponent(SocksProxyComponent proxyComponent) {
        super("SocksRequestReceiver", proxyComponent);
    }

    @Override
    protected void initInternal() {
        log.info("Netty socks server init...");

        socksReceiveGroup = new NioEventLoopGroup(1);

        ServerBootstrap boot = new ServerBootstrap();
        boot.group(socksReceiveGroup, getParentComponent().getWorkerEventLoopGroup())
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    ChannelPipeline cp = socketChannel.pipeline();
                    cp.addLast(new SocksInitRequestDecoder());
                    cp.addLast(new SocksMessageEncoder());
                    cp.addLast(new SocksRequestHandler());
                }

            });

        serverBootstrap = boot;
    }

    @Override
    protected void startInternal() {
        try {
            serverBootstrap.clone().bind(1081).addListener(f -> {
                if(!f.isSuccess()) {
                    Throwable t = f.cause();
                    log.error("bind socks server error.", t);
                    throw new ComponentException(t);
                } else {
                    log.info("Netty socks server complete");
                }
            }).sync();

        } catch (InterruptedException e) {
            throw new ComponentException(e);
        }

        super.startInternal();
    }

    @Override
    protected void stopInternal() {
        socksReceiveGroup.shutdownGracefully();
        super.stopInternal();
    }

    private class SocksRequestHandler extends SimpleChannelInboundHandler<SocksRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, SocksRequest request) {
            switch (request.requestType()) {
                case INIT: {
                    if(log.isTraceEnabled())
                        log.trace("Socks init, thread:" + Thread.currentThread().getName());
                    ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
                    ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.NO_AUTH));
                    break;
                }
                case AUTH: {
                    if(log.isTraceEnabled())
                        log.trace("Socks auth, thread:" + Thread.currentThread().getName());
                    ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
                    ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
                    break;
                }
                case CMD: {
                    SocksCmdRequest req = (SocksCmdRequest) request;
                    if(req.cmdType() != SocksCmdType.CONNECT) {
                        log.info("Socks command request is not connect.");
                        ctx.close();
                        return;
                    }
                    ctx.pipeline().addLast(new SocksCommandRequestHandler()).remove(this);
                    ctx.fireChannelRead(req);
                    break;
                }
                case UNKNOWN: {
                    if(log.isTraceEnabled())
                        log.trace("Unknow socks command, from ip: {}", ctx.channel().localAddress().toString());
                    ctx.close();
                }
            }
        }
    }

    private class SocksCommandRequestHandler extends SimpleChannelInboundHandler<SocksCmdRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, SocksCmdRequest request) {
            String host = request.host();
            int port = request.port();

            if(log.isTraceEnabled())
                log.trace("Socks command request to {}:{}", host, port);

            SocksProxyRequest spq = new SocksProxyRequest(host, port, ctx.channel());

            ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, SocksAddressType.IPv4));
            ctx.pipeline().addLast(new ProxyMessageHandler(spq)).remove(this);
        }
    }

    private class ProxyMessageHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final SocksProxyRequest proxyRequest;

        private ProxyMessageHandler(SocksProxyRequest request) {
            super(false);
            this.proxyRequest = request;
            getParentComponent().pushProxyRequest(proxyRequest);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            getParentComponent().pushProxyMessage(proxyRequest, msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(log.isWarnEnabled())
                log.warn("Exception caught in SocksCommandRequestHandler", cause);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            ctx.pipeline().remove(this);
            ctx.fireChannelInactive();
        }
    }
}
