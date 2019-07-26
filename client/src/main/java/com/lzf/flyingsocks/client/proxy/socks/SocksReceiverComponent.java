package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.client.proxy.ProxyRequest;
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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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

    //UDP代理会话MAP
    private final Map<Integer, UDPProxySession> udpSessionMap = new ConcurrentHashMap<>(32);

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
     * @param host 目标主机名
     * @param port 目标端口号
     * @return 本地UDP代理端口号
     */
    private int processUDPProxyRequest(String host, int port) {
        Bootstrap boot = udpProxyBootstrap.clone()
                .handler(new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel channel) {
                UDPProxySession session = new UDPProxySession(host, port, channel);
                int port = channel.localAddress().getPort();
                udpSessionMap.put(port, session);
                channel.pipeline().addFirst(new UDPProxyMessageHandler(session));
            }
        });

        class Port { private int value = 0; }
        Port p = new Port();

        try {
            boot.bind(0).addListener((ChannelFuture future) -> {
                if(future.isSuccess()) {
                    p.value = ((DatagramChannel)future.channel()).localAddress().getPort();
                } else {
                    log.warn("Bind proxy UDP port occur a exception", future.cause());
                    p.value = -1;
                }
            }).sync();
            return p.value;
        } catch (InterruptedException e) {
            return -1;
        }
    }

    /**
     * UDP代理会话，每个UDP代理端口和UDPProxySession对象一一对应
     */
    static final class UDPProxySession {
        //目标服务器主机名
        final String host;
        //目标服务器IP
        final int port;
        //建立时间
        final long buildTime;
        //绑定的端口
        final int bindPort;
        //UDP Channel对象
        final DatagramChannel channel;

        private UDPProxySession(String host, int port, DatagramChannel channel) {
            this.host = Objects.requireNonNull(host);
            this.port = port;
            this.buildTime = System.currentTimeMillis();
            this.bindPort = channel.localAddress().getPort();
            this.channel = Objects.requireNonNull(channel);
        }

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

                    cp.addFirst(new SocksCmdRequestDecoder());

                    if(!auth)
                        ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.NO_AUTH));
                    else
                        ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.AUTH_PASSWORD));

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
                            String host = ((SocketChannel)ctx.channel()).localAddress().getHostName();
                            int port = processUDPProxyRequest(req.host(), req.port());
                            if(port > 0) {
                                ctx.pipeline().addLast(new UDPStatusHandler(port)).remove(this);
                                ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, SocksAddressType.IPv4, host, port));
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

            SocksProxyRequest spq = new SocksProxyRequest(host, port, ctx.channel(), ProxyRequest.Protocol.TCP);

            ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, SocksAddressType.IPv4));
            ctx.pipeline().addLast(new TCPProxyMessageHandler(spq)).remove(this);
        }
    }

    private class UDPStatusHandler extends ChannelInboundHandlerAdapter {

        private final int bindPort;

        private UDPStatusHandler(int bindPort) {
            this.bindPort = bindPort;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            log.warn("Unsupport message");
            ReferenceCountUtil.release(msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx)  {
            udpSessionMap.get(bindPort).channel.close();
            udpSessionMap.remove(bindPort);
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
            parent.publish(request);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            proxyRequest.messageQueue().offer(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(log.isWarnEnabled())
                log.warn("Exception caught in TCPProxyMessageHandler", cause);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            ctx.pipeline().remove(this);
            ctx.fireChannelInactive();
        }
    }

    private class UDPProxyMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        private final UDPProxySession proxySession;
        private final SocksProxyRequest proxyRequest;

        private UDPProxyMessageHandler(UDPProxySession proxySession) {
            this.proxySession = Objects.requireNonNull(proxySession);
            this.proxyRequest = new SocksProxyRequest(proxySession.host, proxySession.port, proxySession.channel,
                    ProxyRequest.Protocol.UDP);
            parent.publish(proxyRequest);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            proxyRequest.messageQueue().offer(packet.content());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if(proxyRequest.serverChannel() != null)
                proxyRequest.serverChannel().close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(log.isWarnEnabled())
                log.warn("Exception caught in UDPProxyMessageHandler", cause);
        }
    }
}
