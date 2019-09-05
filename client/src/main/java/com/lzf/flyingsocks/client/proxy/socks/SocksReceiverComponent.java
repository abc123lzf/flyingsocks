package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.util.BaseUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socks.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public final class SocksReceiverComponent extends AbstractComponent<SocksProxyComponent> {

    // 引导类
    private ServerBootstrap serverBootstrap;

    // UDP代理引导类
    private Bootstrap udpProxyBootstrap;

    // Netty线程池
    private EventLoopGroup socksReceiveGroup;

    //活跃的UDP代理端口
    private final ConcurrentMap<String, DatagramChannel> udpProxyChannelMap = new ConcurrentHashMap<>(128);

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

    SocksReceiverComponent(SocksProxyComponent proxyComponent) {
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

        socksReceiveGroup = new NioEventLoopGroup(2);

        ServerBootstrap boot = new ServerBootstrap();
        boot.group(socksReceiveGroup, parent.getWorkerEventLoopGroup())
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_SNDBUF, 1024 * 64)    //64KB发送缓冲区
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel channel) {
                    ChannelPipeline cp = channel.pipeline();
                    cp.addLast(new SocksInitRequestDecoder());
                    cp.addLast(new SocksMessageEncoder());
                    cp.addLast(new SocksRequestHandler());
                }
            });

        serverBootstrap = boot;

        Bootstrap udpBoot = new Bootstrap();
        udpBoot.group(socksReceiveGroup)
                .channel(NioDatagramChannel.class);
        this.udpProxyBootstrap = udpBoot;
    }

    @Override
    protected void startInternal() {
        try {
            serverBootstrap.clone().bind(bindAddress, port).addListener(f -> {
                if(!f.isSuccess()) {
                    log.error("bind socks server error, address:[{}:{}]", bindAddress, port, f.cause());
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
        socksReceiveGroup.shutdownGracefully();
        super.stopInternal();
    }

    /**
     * 验证主机名/IP地址
     * @param address 主机名/IP地址
     * @return 是否是合法的主机名/IP地址
     */
    private boolean vaildateAddress(String address) {
        return BaseUtils.isHostName(address) || BaseUtils.isIPv4Address(address);
    }

    /**
     * 构造一个UDP代理端口
     * @param host 客户端IP地址/主机名
     * @param port 客户端端口
     * @return 本地UDP代理端口Channel
     */
    private DatagramChannel processUDPProxyRequest(String host, int port) {
        Bootstrap boot = udpProxyBootstrap.clone()
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel channel) {
                        channel.pipeline().addFirst(UDPProxyMessageDirectSendOutboundHandler.INSTANCE,
                                new UDPProxyMessageHandler(host, port));
                    }
                });

        ChannelFuture future = boot.bind(0).addListener((ChannelFuture f) -> {
            if(f.isSuccess()) {
                log.trace("Bind proxy UDP receive port has done");
            } else {
                log.warn("Bind proxy UDP port occur a exception", f.cause());
            }
        });

        future.awaitUninterruptibly();
        if(future.isSuccess())
            return ((DatagramChannel)future.channel());
        else
            return null;
    }

    /**
     * 负责接收Socks请求
     */
    @ChannelHandler.Sharable
    private class SocksRequestHandler extends SimpleChannelInboundHandler<SocksRequest> {
        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, final SocksRequest request) {
            final ChannelPipeline cp = ctx.pipeline();

            switch (request.requestType()) {
                case INIT: {  //如果是Socks5初始化请求
                    if(log.isTraceEnabled())
                        log.trace("Socks init, thread:" + Thread.currentThread().getName());

                    if(!auth) {
                        cp.addFirst(new SocksCmdRequestDecoder());
                        ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.NO_AUTH));
                    } else {
                        ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.AUTH_PASSWORD));
                    }

                    break;
                }
                case AUTH: {  //如果是Socks5认证请求
                    if(log.isTraceEnabled())
                        log.trace("Socks auth, thread:" + Thread.currentThread().getName());
                    if(!(cp.first() instanceof SocksCmdRequestDecoder))
                        cp.addFirst(new SocksCmdRequestDecoder());

                    if(!auth) {
                        ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
                    } else {
                        SocksAuthRequest req = (SocksAuthRequest) request;
                        if(req.username().equals(username) && req.password().equals(password)) {
                            ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
                        } else {
                            ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.FAILURE));
                        }
                    }

                    break;
                }
                case CMD: {  //如果是Socks5命令请求
                    SocksCmdRequest req = (SocksCmdRequest) request;
                    if(!vaildateAddress(req.host())) {  //如果主机名/IP地址格式有误
                        ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.ADDRESS_NOT_SUPPORTED, SocksAddressType.IPv4));
                        ctx.close();
                        return;
                    }

                    SocksCmdType type = req.cmdType();
                    switch (type) {
                        case CONNECT: {
                            processTCPProxyRequest(ctx, req);
                        } break;

                        case UDP: {
                            String localHost = ((SocketChannel)ctx.channel()).localAddress().getHostString();
                            DatagramChannel udpCh = processUDPProxyRequest(req.host(), req.port());
                            if(udpCh != null) {
                                ctx.pipeline().remove(this);
                                ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, SocksAddressType.IPv4, localHost, udpCh.localAddress().getPort()));
                                String id = ctx.channel().id().asShortText();
                                udpProxyChannelMap.put(id, udpCh);
                                cp.addLast(new ChannelInboundHandlerAdapter() {  //用于释放UDP端口
                                    @Override
                                    public void channelInactive(ChannelHandlerContext chc) {
                                        Channel ch = udpProxyChannelMap.get(id);
                                        ch.close();
                                        udpProxyChannelMap.remove(id);
                                    }
                                });
                            } else {
                                ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.FAILURE, SocksAddressType.IPv4));
                                ctx.close();
                            }
                        } break;

                        default: {
                            log.info("Socks command request is not CONNECT or UDP.");
                            ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.COMMAND_NOT_SUPPORTED, SocksAddressType.IPv4));
                            ctx.close();
                            return;
                        }
                    }

                    break;
                }
                case UNKNOWN: {  //未知请求关闭连接
                    if(log.isInfoEnabled())
                        log.info("Unknown socks command, from ip: {}", ctx.channel().localAddress().toString());
                    ctx.close();
                }
            }
        }

        private void processTCPProxyRequest(ChannelHandlerContext ctx, SocksCmdRequest request) {
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
     * 负责接收客户端要求代理的数据，仅TCP代理
     */
    private class TCPProxyMessageHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final SocksProxyRequest proxyRequest;

        private TCPProxyMessageHandler(SocksProxyRequest request) {
            super(false);
            this.proxyRequest = request;
            parent.publish(request);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            proxyRequest.messageQueue().offer(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(cause instanceof IOException) {
                if(log.isInfoEnabled())
                    log.info("Local client close connection, from {}", ctx.channel().remoteAddress());
                ctx.close();
            } else if(log.isWarnEnabled())
                log.warn("Exception caught in TCPProxyMessageHandler", cause);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            proxyRequest.setCtl(31, true);
            ctx.pipeline().remove(this);
            ctx.fireChannelInactive();
        }
    }

    /**
     * 负责接收客户端要求代理的数据，仅UDP代理
     */
    private class UDPProxyMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        private final InetSocketAddress receiveAddress;

        private UDPProxyMessageHandler(String host, int port) {
            super(false);
            this.receiveAddress = new InetSocketAddress(host, port);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            if(log.isTraceEnabled()) {
                DatagramChannel ch = (DatagramChannel) ctx.channel();
                log.trace("Local UDP Proxy receive port is active, port: {}, Receive address: {}",
                        ch.localAddress().getPort(), receiveAddress);
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            InetSocketAddress sender = packet.sender();
            if(sender.equals(receiveAddress)) {  //如果该UDP包是本地应用程序发出的
                ByteBuf buf = packet.content();

                final int head = buf.readInt();
                final String ip;
                final int port;
                String host;

                if (head == 0x01) {
                    ip = BaseUtils.parseIntToIPv4Address(buf.readInt());
                    host = ip;
                    port = BaseUtils.parseUnsignedShortToInteger(buf.readShort());
                } else if (head == 0x03) {
                    int len = BaseUtils.parseByteToInteger(buf.readByte());
                    host = buf.readCharSequence(len, Charset.forName("ASCII")).toString();
                    port = BaseUtils.parseUnsignedShortToInteger(buf.readShort());
                    InetSocketAddress haddr = new InetSocketAddress(host, port);
                    InetAddress add = haddr.getAddress();
                    if (add != null) {
                        ip = BaseUtils.parseByteArrayToIPv4Address(add.getAddress());
                    } else {
                        if (log.isWarnEnabled())
                            log.warn("Unknown domain name: {}", host);
                        return;
                    }
                } else {
                    packet.release();
                    return;
                }

                DatagramProxyRequest request = new DatagramProxyRequest(ip, port, (DatagramChannel) ctx.channel(), sender);
                request.setContent(buf);
                assert parent != null;
                if (parent.needProxy(host)) {
                    parent.publish(request);
                } else {
                    ctx.writeAndFlush(request);
                }

                if (log.isTraceEnabled())
                    log.trace("UDP Packet send to SenderComponent, content: {}", request.toString());
            } else {
                int ip = BaseUtils.parseByteArrayToIPv4Integer(sender.getAddress().getAddress());
                CompositeByteBuf buf = Unpooled.compositeBuffer();
                ByteBuf head = Unpooled.buffer(4 + 4 + 2);
                head.writeInt(0x01);
                head.writeInt(ip);
                head.writeShort(sender.getPort());
                buf.addComponents(true, head, packet.content());

                DatagramPacket dp = new DatagramPacket(buf, receiveAddress);
                ctx.writeAndFlush(dp);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("Local UDP Proxy receive port are close");
        }
    }

    @ChannelHandler.Sharable
    private static class UDPProxyMessageDirectSendOutboundHandler extends ChannelOutboundHandlerAdapter {
        private static final UDPProxyMessageDirectSendOutboundHandler INSTANCE = new UDPProxyMessageDirectSendOutboundHandler();

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            if(msg instanceof DatagramProxyRequest) {
                DatagramProxyRequest req = (DatagramProxyRequest) msg;
                InetSocketAddress remote = new InetSocketAddress(req.getHost(), req.getPort());
                ByteBuf buf = req.takeClientMessage();
                assert buf != null;
                DatagramPacket packet = new DatagramPacket(buf, remote);
                ctx.write(packet, ctx.voidPromise());
            } else {
                ctx.write(msg, ctx.voidPromise());
            }
        }

        private UDPProxyMessageDirectSendOutboundHandler() {
            if(INSTANCE != null)
                throw new UnsupportedOperationException();
        }
    }
}
