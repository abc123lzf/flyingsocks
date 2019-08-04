package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.util.BaseUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socks.*;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

public final class SocksReceiverComponent extends AbstractComponent<SocksProxyComponent> {

    //主机名正则表达式
    private static final Pattern HOST_PATTERN = Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");

    //IP地址正则表达式
    private static final Pattern IP_PATTERN = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");

    // 引导类
    private ServerBootstrap serverBootstrap;

    // UDP代理引导类
    private Bootstrap udpProxyBootstrap;

    // Netty线程池
    private EventLoopGroup socksReceiveGroup;

    //活跃的UDP代理端口
    private volatile DatagramChannel udpProxyChannel;

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
            CountDownLatch latch = new CountDownLatch(2);

            Bootstrap boot = udpProxyBootstrap.clone()
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel channel) {
                            channel.pipeline().addFirst(new UDPProxyMessageHandler());
                        }
                    });

            boot.bind(0).addListener((ChannelFuture future) -> {
                if(future.isSuccess()) {
                    log.info("Bind proxy UDP receive port has done");
                } else {
                    log.warn("Bind proxy UDP port occur a exception", future.cause());
                }

                latch.countDown();
            });

            serverBootstrap.clone().bind(bindAddress, port).addListener(f -> {
                if(!f.isSuccess()) {
                    Throwable t = f.cause();
                    log.error("bind socks server error, address:[{}:{}]", bindAddress, port, t);
                    System.exit(1);
                } else {
                    log.info("Netty socks server complete");
                }

                latch.countDown();
            });

            latch.await();
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
        return HOST_PATTERN.matcher(address).matches() ||
                IP_PATTERN.matcher(address).matches();
    }

    /**
     * 构造一个UDP代理端口
     * @param host 客户端IP地址/主机名
     * @param port 客户端端口
     * @return 本地UDP代理端口号
     */
    private int processUDPProxyRequest(String host, int port) {
        return udpProxyChannel.localAddress().getPort();
    }

    /**
     * 负责接收Socks请求
     */
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
                            int port = processUDPProxyRequest(req.host(), req.port());
                            if(port > 0) {
                                ctx.pipeline().remove(this);
                                ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, SocksAddressType.IPv4, localHost, port));
                            } else {
                                ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.FAILURE, SocksAddressType.IPv4));
                                ctx.close();
                            }
                        } break;

                        default: {
                            if(log.isInfoEnabled())
                                log.info("Socks command request is not connect.");
                            ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.COMMAND_NOT_SUPPORTED, SocksAddressType.IPv4));
                            ctx.close();
                            return;
                        }
                    }

                    break;
                }
                case UNKNOWN: {  //未知请求关闭连接
                    if(log.isInfoEnabled())
                        log.info("Unknow socks command, from ip: {}", ctx.channel().localAddress().toString());
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
            ctx.pipeline().remove(this);
            ctx.fireChannelInactive();
        }
    }

    /**
     * 负责接收客户端要求代理的数据，仅UDP代理
     */
    private class UDPProxyMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        private UDPProxyMessageHandler() {
            super(false);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("Local UDP Proxy receive port is active");
            udpProxyChannel = (DatagramChannel) ctx.channel();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            InetSocketAddress sender = packet.sender();
            ByteBuf buf = packet.content();

            int head = buf.readInt();
            String ip;
            int port;
            if(head == 0x01) {
                ip = BaseUtils.parseIntToIPv4Address(buf.readInt());
                port = BaseUtils.parseUnsignedShortToInteger(buf.readShort());
            } else if(head == 0x03) {
                int len = BaseUtils.parseByteToInteger(buf.readByte());
                String host = buf.readCharSequence(len, Charset.forName("ASCII")).toString();
                port = BaseUtils.parseUnsignedShortToInteger(buf.readShort());
                InetSocketAddress haddr = new InetSocketAddress(host, port);
                InetAddress add = haddr.getAddress();
                if(add != null) {
                    ip = BaseUtils.parseByteArrayToIPv4Address(add.getAddress());
                } else {
                    if(log.isWarnEnabled())
                        log.warn("Unknown domain name: {}", host);
                    return;
                }
            } else {
                ReferenceCountUtil.release(packet);
                return;
            }

            DatagramProxyRequest request = new DatagramProxyRequest(ip, port, (DatagramChannel) ctx.channel(), sender);
            request.setContent(buf);
            getParentComponent().publish(request);

            if(log.isTraceEnabled())
                log.trace("UDP Packet send to SenderComponent, content: {}", request.toString());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("Local UDP Proxy receive port are close");
            udpProxyChannel = null;
        }
    }
}
