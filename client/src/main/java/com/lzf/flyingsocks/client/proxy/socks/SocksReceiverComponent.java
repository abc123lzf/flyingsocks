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

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.client.proxy.ProxyComponent;
import com.lzf.flyingsocks.client.proxy.ProxyRequest;
import com.lzf.flyingsocks.util.BaseUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socks.SocksAddressType;
import io.netty.handler.codec.socks.SocksAuthRequest;
import io.netty.handler.codec.socks.SocksAuthRequestDecoder;
import io.netty.handler.codec.socks.SocksAuthResponse;
import io.netty.handler.codec.socks.SocksAuthScheme;
import io.netty.handler.codec.socks.SocksAuthStatus;
import io.netty.handler.codec.socks.SocksCmdRequest;
import io.netty.handler.codec.socks.SocksCmdRequestDecoder;
import io.netty.handler.codec.socks.SocksCmdResponse;
import io.netty.handler.codec.socks.SocksCmdStatus;
import io.netty.handler.codec.socks.SocksInitRequestDecoder;
import io.netty.handler.codec.socks.SocksInitResponse;
import io.netty.handler.codec.socks.SocksMessageEncoder;
import io.netty.handler.codec.socks.SocksRequest;

import java.util.Objects;

/**
 * Socks5代理请求组件
 * 负责处理本地的Socks5代理请求
 */
public final class SocksReceiverComponent extends AbstractComponent<ProxyComponent> {

    // UDP代理引导类
    private Bootstrap udpProxyBootstrap;

    // Netty线程池
    private EventLoopGroup eventLoopGroup;

    //认证策略
    private volatile AuthenticationStrategy authenticationStrategy;

    // 绑定的端口
    private int port;

    // 绑定的IP地址，如果需要对外网开放则为0.0.0.0
    private String bindAddress;


    public SocksReceiverComponent(ProxyComponent proxyComponent) {
        super("SocksRequestReceiver", Objects.requireNonNull(proxyComponent));
    }

    @Override
    protected void initInternal() {
        SocksConfig cfg = getConfigManager().getConfig(SocksConfig.NAME, SocksConfig.class);

        this.port = cfg.getPort();
        this.bindAddress = cfg.getAddress();

        if (cfg.isAuth()) {
            this.authenticationStrategy = new SimpleAuthenticationStrategy(cfg.getUsername(), cfg.getPassword());
        }

        parent.getParentComponent().registerConfigEventListener(event -> {
            if (event.getSource() instanceof SocksConfig && event.getEvent().equals(Config.UPDATE_EVENT)) {
                if (!cfg.isAuth()) {
                    this.authenticationStrategy = null;
                    return;
                }

                this.authenticationStrategy = new SimpleAuthenticationStrategy(cfg.getUsername(), cfg.getPassword());
            }
        });

        this.eventLoopGroup = parent.createNioEventLoopGroup(4);
        Bootstrap udpBoot = new Bootstrap();
        udpBoot.group(eventLoopGroup).channel(NioDatagramChannel.class);
        this.udpProxyBootstrap = udpBoot;
    }

    @Override
    protected void startInternal() {
        try {
            ServerBootstrap boot = new ServerBootstrap();
            boot.group(eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            ChannelPipeline cp = channel.pipeline();
                            cp.addLast(new SocksInitRequestDecoder());  //Socks5初始化(INIT)请求解码器
                            cp.addLast(new SocksMessageEncoder());      //Socks5响应编码器
                            cp.addLast(new SocksRequestHandler());      //Socks5请求处理器
                        }
                    });

            boot.bind(bindAddress, port).addListener(f -> {
                if (!f.isSuccess()) {
                    log.error("Socks server bind failure, address:[{}:{}]", bindAddress, port, f.cause());
                    System.exit(1);
                } else {
                    log.info("Netty socks server complete");
                }
            }).await();
        } catch (InterruptedException e) {
            throw new ComponentException(e);
        }

        super.startInternal();
    }

    @Override
    protected void stopInternal() {
        eventLoopGroup.shutdownGracefully();
        super.stopInternal();
    }

    /**
     * 验证主机名/IP地址
     *
     * @param address 主机名/IP地址
     * @return 是否是合法的主机名/IP地址
     */
    private boolean vaildateAddress(String address) {
        return BaseUtils.isHostName(address) || BaseUtils.isIPAddress(address);
    }


    /**
     * 构造一个UDP代理端口
     *
     * @param closeFuture Socks5代理请求连接的CloseFuture
     * @return 本地UDP代理端口ChannelFuture
     */

    private ChannelFuture bindUdpTunnelService(ChannelFuture closeFuture) {
        Bootstrap boot = udpProxyBootstrap.clone()
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel channel) {
                        channel.pipeline()
                                .addFirst(new UdpProxyMessageDecoder())
                                .addLast(new UdpProxyMessageHandler(parent));
                    }
                });

        return boot.bind(0).addListener((ChannelFuture bindFuture) -> {
            if (bindFuture.isSuccess()) {
                //log.trace("Bind proxy UDP receive port has done");
                closeFuture.addListener((ChannelFuture future) -> {
                    //log.trace("Close UDP Proxy port");
                    bindFuture.channel().close();
                });
                if (!closeFuture.channel().isActive()) {
                    bindFuture.channel().close();
                }
            } else {
                log.warn("Bind proxy UDP port occur a exception", bindFuture.cause());
            }
        });
    }

    /**
     * 负责接收Socks请求
     */
    private class SocksRequestHandler extends SimpleChannelInboundHandler<SocksRequest> {

        private final AuthenticationStrategy authenticationStrategy;

        private boolean passAuth = false;

        SocksRequestHandler() {
            this.authenticationStrategy = SocksReceiverComponent.this.authenticationStrategy;
            if (this.authenticationStrategy == null) {
                passAuth = true;
            }
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, final SocksRequest request) {
            final ChannelPipeline cp = ctx.pipeline();

            switch (request.requestType()) {
                case INIT: {  //如果是Socks5初始化请求
                    log.trace("Socks init");
                    if (authenticationStrategy == null) {
                        cp.addFirst(new SocksCmdRequestDecoder());
                        ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.NO_AUTH));
                    } else {
                        cp.addFirst(new SocksAuthRequestDecoder());
                        ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.AUTH_PASSWORD));
                    }

                    break;
                }
                case AUTH: {  //如果是Socks5认证请求
                    processAuthRequest(ctx, (SocksAuthRequest) request);
                    break;
                }
                case CMD: {  //如果是Socks5命令请求
                    if (!passAuth) {
                        ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.FORBIDDEN, SocksAddressType.IPv4))
                                .addListener(ChannelFutureListener.CLOSE);
                        return;
                    }

                    processCommandRequest(ctx, (SocksCmdRequest) request);
                    break;
                }
                case UNKNOWN: {  //未知请求关闭连接
                    if (log.isInfoEnabled()) {
                        log.info("Unknown socks command, from ip: {}", ctx.channel().localAddress().toString());
                    }
                    ctx.close();
                }
            }
        }


        private void processAuthRequest(ChannelHandlerContext ctx, SocksAuthRequest request) {
            log.trace("Socks auth");
            ChannelPipeline cp = ctx.pipeline();
            String username = request.username();
            String password = request.password();

            if (authenticationStrategy == null) {
                if (cp.get(SocksCmdRequestDecoder.class) == null) {
                    cp.addFirst(new SocksCmdRequestDecoder());
                }
                ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
                return;
            }

            log.info("Socks auth, user:{} pass:{}", username, password);

            if (authenticationStrategy.grantAuthorization(username, password)) {
                this.passAuth = true;
                cp.addFirst(new SocksCmdRequestDecoder());
                authenticationStrategy.afterAuthorizationSuccess(cp, request.username());
                ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
            } else {
                ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.FAILURE))
                        .addListener(ChannelFutureListener.CLOSE);
            }
        }


        private void processCommandRequest(ChannelHandlerContext ctx, SocksCmdRequest request) {
            String host = request.host();
            int port = request.port();

            log.trace("Socks command request to {}:{}", host, port);

            if (!vaildateAddress(host)) {  //如果主机名/IP地址格式有误
                log.info("Illegal proxy host {}", host);
                ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.ADDRESS_NOT_SUPPORTED, SocksAddressType.IPv4))
                        .addListener(ChannelFutureListener.CLOSE);
                ctx.close();
                return;
            }

            switch (request.cmdType()) {
                case CONNECT: {
                    ProxyRequest pr = new ProxyRequest(host, port, ctx.channel(), ProxyRequest.Protocol.TCP);
                    ctx.pipeline().addLast(new TcpProxyMessageHandler(pr, parent)).remove(this);
                    ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, SocksAddressType.IPv4));
                }
                break;

                case UDP: {
                    ChannelFuture future = bindUdpTunnelService(ctx.channel().closeFuture());
                    future.addListener(ch -> {
                        if (!ch.isSuccess()) {
                            ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.FAILURE, SocksAddressType.IPv4));
                            ctx.close();
                        }

                        ctx.pipeline().remove(this);
                        ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, SocksAddressType.IPv4,
                                null, ((DatagramChannel) future.channel()).localAddress().getPort()));
                    });
                }
                break;

                default: {
                    log.info("Socks command request is not CONNECT or UDP.");
                    ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.COMMAND_NOT_SUPPORTED, SocksAddressType.IPv4));
                    ctx.close();
                }
            }
        }
    }
}
