package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.client.proxy.ProxyComponent;
import com.lzf.flyingsocks.client.proxy.ProxyRequest;
import com.lzf.flyingsocks.util.BaseUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
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
import io.netty.handler.codec.socks.SocksCmdType;
import io.netty.handler.codec.socks.SocksInitRequestDecoder;
import io.netty.handler.codec.socks.SocksInitResponse;
import io.netty.handler.codec.socks.SocksMessageEncoder;
import io.netty.handler.codec.socks.SocksRequest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Socks5代理请求组件
 * 负责处理本地的Socks5代理请求
 */
public final class SocksReceiverComponent extends AbstractComponent<ProxyComponent> {

    // 引导类
    private ServerBootstrap serverBootstrap;

    // UDP代理引导类
    private Bootstrap udpProxyBootstrap;

    // Netty线程池
    private EventLoopGroup ioEventLoop;

    //活跃的UDP代理端口
    private final ConcurrentMap<String, DatagramChannel> udpProxyChannelMap = new ConcurrentHashMap<>(128);

    // 是否进行Socks认证
    private volatile boolean auth;

    // 绑定的端口
    private int port;

    // 绑定的IP地址，如果需要对外网开放则为0.0.0.0
    private String bindAddress;

    // Socks5认证用户名，如果无需认证则为null
    private volatile String username;

    // Socks5认证密码，如果无需认证则为null
    private volatile String password;

    public SocksReceiverComponent(ProxyComponent proxyComponent) {
        super("SocksRequestReceiver", Objects.requireNonNull(proxyComponent));
    }

    @Override
    protected void initInternal() {
        SocksConfig cfg = parent.getParentComponent().getConfigManager().getConfig(SocksConfig.NAME, SocksConfig.class);

        this.auth = cfg.isAuth();
        this.port = cfg.getPort();
        this.username = cfg.getUsername();
        this.password = cfg.getPassword();
        this.bindAddress = cfg.getAddress();

        parent.getParentComponent().registerConfigEventListener(event -> {
            if (event.getSource() instanceof SocksConfig && event.getEvent().equals(Config.UPDATE_EVENT)) {
                this.username = cfg.getUsername();
                this.password = cfg.getPassword();
                this.auth = cfg.isAuth();
            }
        });

        this.ioEventLoop = parent.createNioEventLoopGroup(4);

        ServerBootstrap boot = new ServerBootstrap();
        boot.group(ioEventLoop)
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

        this.serverBootstrap = boot;

        Bootstrap udpBoot = new Bootstrap();
        udpBoot.group(ioEventLoop).channel(NioDatagramChannel.class);
        this.udpProxyBootstrap = udpBoot;
    }

    @Override
    protected void startInternal() {
        try {
            serverBootstrap.clone().bind(bindAddress, port).addListener(f -> {
                if (!f.isSuccess()) {
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
        ioEventLoop.shutdownGracefully();
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
     * @param host 客户端IP地址/主机名
     * @param port 客户端端口
     * @return 本地UDP代理端口Channel
     */

    private DatagramChannel processUDPProxyRequest(String host, int port) {
        /*Bootstrap boot = udpProxyBootstrap.clone()
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel channel) {
                        channel.pipeline().addFirst(UDPProxyMessageDirectSendOutboundHandler.INSTANCE,
                                new UDPProxyMessageHandler(host, port));
                    }
                });

        ChannelFuture future = boot.bind(0).addListener((ChannelFuture f) -> {
            if (f.isSuccess()) {
                log.trace("Bind proxy UDP receive port has done");
            } else {
                log.warn("Bind proxy UDP port occur a exception", f.cause());
            }
        });

        future.awaitUninterruptibly();
        return future.isSuccess() ? ((DatagramChannel) future.channel()) : null;*/
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
                    log.trace("Socks init");
                    if (!auth) {
                        cp.addFirst(new SocksCmdRequestDecoder());
                        ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.NO_AUTH));
                    } else {
                        cp.addFirst(new SocksAuthRequestDecoder());
                        ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.AUTH_PASSWORD));
                    }

                    break;
                }
                case AUTH: {  //如果是Socks5认证请求
                    log.trace("Socks auth");
                    if (!auth) {
                        cp.addFirst(new SocksCmdRequestDecoder());
                        ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
                    } else {
                        SocksAuthRequest req = (SocksAuthRequest) request;
                        if (log.isInfoEnabled())
                            log.info("Socks auth, user:{} pass:{}", req.username(), req.password());

                        if (req.username().equals(username) && req.password().equals(password)) {
                            cp.addFirst(new SocksCmdRequestDecoder());
                            ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
                        } else {
                            ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.FAILURE));
                        }
                    }

                    break;
                }
                case CMD: {  //如果是Socks5命令请求
                    SocksCmdRequest req = (SocksCmdRequest) request;
                    log.trace("Socks command");
                    if (!vaildateAddress(req.host())) {  //如果主机名/IP地址格式有误
                        log.info("Illegal proxy host {}", req.host());
                        ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.ADDRESS_NOT_SUPPORTED, SocksAddressType.IPv4));
                        ctx.close();
                        return;
                    }

                    SocksCmdType type = req.cmdType();
                    switch (type) {
                        case CONNECT: {
                            processTCPProxyRequest(ctx, req);
                        }
                        break;

                        /*case UDP: {
                            String localHost = ((SocketChannel) ctx.channel()).localAddress().getHostString();
                            DatagramChannel udpCh = processUDPProxyRequest(((SocketChannel) ctx.channel()).remoteAddress().getHostName(), req.port());
                            if (udpCh != null) {
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
                        }
                        break;*/

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
                    if (log.isInfoEnabled())
                        log.info("Unknown socks command, from ip: {}", ctx.channel().localAddress().toString());
                    ctx.close();
                }
            }
        }

        private void processTCPProxyRequest(ChannelHandlerContext ctx, SocksCmdRequest request) {
            String host = request.host();
            int port = request.port();

            log.trace("Socks command request to {}:{}", host, port);

            ProxyRequest pr = new ProxyRequest(host, port, ctx.channel(), ProxyRequest.Protocol.TCP);
            ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, SocksAddressType.IPv4));
            ctx.pipeline().addLast(new TCPProxyMessageHandler(pr)).remove(this);
        }
    }

    /**
     * 负责接收客户端要求代理的数据，仅TCP代理
     */
    class TCPProxyMessageHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final ProxyRequest proxyRequest;

        TCPProxyMessageHandler(ProxyRequest request) {
            super(false);
            this.proxyRequest = request;
            parent.publish(request);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws IOException {
            //proxyRequest.messageQueue().offer(msg);
            try {
                proxyRequest.transferClientMessage(msg);
            } catch (Throwable e) {
                if (msg.refCnt() > 0) {
                    msg.release();
                }
                throw e;
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof IOException) {
                log.info("Local client close connection, from {}", ctx.channel().remoteAddress());
                ctx.close();
            } else if (log.isWarnEnabled()) {
                log.warn("Exception caught in TCPProxyMessageHandler", cause);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            proxyRequest.close();
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
            if (log.isTraceEnabled()) {
                DatagramChannel ch = (DatagramChannel) ctx.channel();
                log.trace("Local UDP Proxy receive port is active, port: {}, Receive address: {}",
                        ch.localAddress().getPort(), receiveAddress);
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws IOException {
            InetSocketAddress sender = packet.sender();
            log.debug("Receive UDP Package");
            if (sender.equals(receiveAddress)) {  //如果该UDP包是本地应用程序发出的
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
                    host = buf.readCharSequence(len, StandardCharsets.US_ASCII).toString();
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

                ProxyRequest request = new DatagramProxyRequest(ip, port, (DatagramChannel) ctx.channel(), sender);
                try {
                    request.transferClientMessage(buf);
                } catch (IOException e) {
                    request.close();
                    throw e;
                }

                assert parent != null;
                if (parent.needProxy(host)) {
                    parent.publish(request);
                } else {
                    ctx.writeAndFlush(request);
                }

                if (log.isTraceEnabled()) {
                    log.trace("UDP Packet send to SenderComponent, content: {}", request.toString());
                }

            } else {
                int ip = BaseUtils.parseByteArrayToIPv4Integer(sender.getAddress().getAddress());
                CompositeByteBuf buf = PooledByteBufAllocator.DEFAULT.compositeBuffer();
                ByteBuf head = PooledByteBufAllocator.DEFAULT.buffer(4 + 4 + 2);
                head.writeInt(0x01);
                head.writeInt(ip);
                head.writeShort(sender.getPort());
                buf.addComponents(true, head, packet.content());

                DatagramPacket dp = new DatagramPacket(buf, receiveAddress, ((DatagramChannel) ctx.channel()).localAddress());
                ctx.writeAndFlush(dp);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("Local UDP Proxy receive port are close");
        }
    }

    /*@ChannelHandler.Sharable
    private static class UDPProxyMessageDirectSendOutboundHandler extends ChannelOutboundHandlerAdapter {
        private static final UDPProxyMessageDirectSendOutboundHandler INSTANCE = new UDPProxyMessageDirectSendOutboundHandler();

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            if (msg instanceof DatagramProxyRequest) {
                DatagramProxyRequest req = (DatagramProxyRequest) msg;
                InetSocketAddress remote = new InetSocketAddress(req.getHost(), req.getPort());
                ByteBuf buf = req.takeClientMessage();
                assert buf != null;
                DatagramPacket packet = new DatagramPacket(buf, remote, ((DatagramChannel) ctx.channel()).localAddress());
                ctx.write(packet, ctx.voidPromise());
            } else {
                ctx.write(msg, ctx.voidPromise());
            }
        }

        private UDPProxyMessageDirectSendOutboundHandler() {
            if (INSTANCE != null)
                throw new UnsupportedOperationException();
        }
    }*/
}
