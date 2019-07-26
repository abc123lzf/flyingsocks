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

import java.util.Objects;

public final class SocksReceiverComponent extends AbstractComponent<SocksProxyComponent> {

    // 引导类
    private ServerBootstrap serverBootstrap;

    // Netty线程池
    private EventLoopGroup socksReceiveGroup;

    // 是否进行Socks认证
    private boolean auth;

    // 绑定的端口
    private int port;

    // 绑定的IP地址，如果需要对外网开放则为0.0.0.0
    private String bindAddress;

    // Socks5认证用户名，如果无需认证则为null
    private String username;

    // Socks5认证密码，如果无需认证则为null
    private String password;

    public SocksReceiverComponent(SocksProxyComponent proxyComponent) {
        super("SocksRequestReceiver", Objects.requireNonNull(proxyComponent));
    }

    @Override
    protected void initInternal() {
        SocksConfig cfg = parent.getParentComponent().getConfigManager()
                .getConfig(SocksConfig.NAME, SocksConfig.class);

        this.auth = cfg.isAuth();
        this.port = cfg.getPort();
        this.username = cfg.getUsername();
        this.password = cfg.getPassword();
        this.bindAddress = cfg.getAddress();

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
            serverBootstrap.clone().bind(bindAddress, port).addListener(f -> {
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

    /**
     * 负责接收Socks请求
     */
    private class SocksRequestHandler extends SimpleChannelInboundHandler<SocksRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, SocksRequest request) {
            switch (request.requestType()) {
                case INIT: {
                    if(log.isTraceEnabled())
                        log.trace("Socks init, thread:" + Thread.currentThread().getName());

                    if(!auth) {
                        ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
                        ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.NO_AUTH));
                    } else {
                        ctx.pipeline().addFirst(new SocksAuthRequestDecoder());
                        ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.AUTH_PASSWORD));
                    }

                    break;
                }
                case AUTH: {
                    if(log.isTraceEnabled())
                        log.trace("Socks auth, thread:" + Thread.currentThread().getName());

                    ctx.pipeline().addFirst(new SocksCmdRequestDecoder());

                    if(!auth)
                        ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
                    else {
                        SocksAuthRequest req = (SocksAuthRequest) request;
                        if(req.username().equals(username) && req.password().equals(password)) {
                            ctx.pipeline().addFirst(new SocksCmdRequestDecoder()).remove(SocksAuthRequestDecoder.class);
                            ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
                        } else {
                            ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.FAILURE));
                        }
                    }

                    break;
                }
                case CMD: {
                    SocksCmdRequest req = (SocksCmdRequest) request;


                    if(req.cmdType() != SocksCmdType.CONNECT) {
                        if(log.isInfoEnabled())
                            log.info("Socks command request is not connect.");
                        ctx.close();
                        return;
                    }

                    ctx.pipeline().addLast(new SocksCommandRequestHandler()).remove(this);
                    ctx.fireChannelRead(req);

                    break;
                }
                case UNKNOWN: {
                    if(log.isInfoEnabled())
                        log.info("Unknow socks command, from ip: {}", ctx.channel().localAddress().toString());
                    ctx.close();
                }
            }
        }
    }

    /**
     * 负责接收Socks命令
     */
    private class SocksCommandRequestHandler extends SimpleChannelInboundHandler<SocksCmdRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, SocksCmdRequest request) {
            String host = request.host();
            int port = request.port();

            if(log.isTraceEnabled())
                log.trace("Socks command request to {}:{}", host, port);

            SocksProxyRequest spq = new SocksProxyRequest(host, port, ctx.channel());

            ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, SocksAddressType.IPv4));
            ctx.pipeline().addLast(new TCPProxyMessageHandler(spq)).remove(this);
        }
    }

    /**
     * 负责接收客户端要求代理的数据
     */
    private class TCPProxyMessageHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final SocksProxyRequest proxyRequest;

        private TCPProxyMessageHandler(SocksProxyRequest request) {
            super(false);
            this.proxyRequest = request;
            getParentComponent().publish(request);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            proxyRequest.getMessageQueue().offer(msg);
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
