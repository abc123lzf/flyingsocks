package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.client.proxy.ProxyRequest;
import com.lzf.flyingsocks.client.proxy.ProxyRequestSubscriber;
import com.lzf.flyingsocks.util.BaseUtils;
import com.lzf.flyingsocks.util.CommonChannelOutboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.locks.LockSupport;

public final class SocksSenderComponent extends AbstractComponent<SocksProxyComponent> {

    /**
     * TCP代理连接引导模板
     */
    private final Bootstrap connectBootstrap;

    /**
     * UDP代理连接引导模板
     */
    private final Bootstrap bindBootstrap;

    /**
     * 用于发送UDP代理消息
     */
    private volatile DatagramChannel udpProxyChannel;

    /**
     * TCP代理订阅者
     */
    private TCPSocksProxyRequestSubscriber tcpRequsetSubscriber;

    /**
     * UDP代理订阅者
     */
    private UDPSocksProxyRequestSubscriber udpRequestSubscriber;


    SocksSenderComponent(SocksProxyComponent proxyComponent) {
        super("SocksSender", proxyComponent);
        connectBootstrap = new Bootstrap();
        connectBootstrap.group(parent.getWorkerEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.AUTO_CLOSE, true);

        bindBootstrap = new Bootstrap();
        bindBootstrap.group(parent.getWorkerEventLoopGroup())
                .channel(NioDatagramChannel.class);
    }

    @Override
    protected void initInternal() {
        Thread t = Thread.currentThread();
        Bootstrap boot = bindBootstrap.clone().handler(new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel ch) {
                ch.pipeline().addLast(new UDPHandler());
            }
        });

        boot.bind(0).addListener((ChannelFuture future) -> {
            if(future.isSuccess()) {
                this.udpProxyChannel = (DatagramChannel) future.channel();
                LockSupport.unpark(t);
            } else {
                log.error("Can not bind UDP Proxy Port, cause:", future.cause());
                System.exit(1);
            }
        });

        LockSupport.park();
    }

    @Override
    protected void startInternal() {
        parent.registerSubscriber(this.tcpRequsetSubscriber = new TCPSocksProxyRequestSubscriber());
        parent.registerSubscriber(this.udpRequestSubscriber = new UDPSocksProxyRequestSubscriber());
    }


    @Override
    protected void stopInternal() {
        parent.removeSubscriber(tcpRequsetSubscriber);
        parent.removeSubscriber(udpRequestSubscriber);
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


    private final class UDPSocksProxyRequestSubscriber implements ProxyRequestSubscriber {
        @Override
        public void receive(final ProxyRequest req) {
            final DatagramProxyRequest request = (DatagramProxyRequest) req;
            final String host = request.getHost();
            final int port = request.getPort();

            if(log.isTraceEnabled())
                log.trace("ready to send Datagram to server {}:{} ...", host, port);

            udpProxyChannel.writeAndFlush(request);
        }

        @Override
        public boolean receiveNeedlessProxy() {
            return true;
        }

        @Override
        public boolean receiveNeedProxy() {
            return false;
        }

        @Override
        public Set<ProxyRequest.Protocol> requestProtcol() {
            return ONLY_UDP;
        }

        @Override
        public Class<? extends ProxyRequest> requestType() {
            return DatagramProxyRequest.class;
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
                log.info("Direct TCP Connection force to close, from remote server {}:{}", request.getHost(), request.getPort());
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

    private final class UDPHandler extends SimpleChannelInboundHandler<DatagramPacket> implements CommonChannelOutboundHandler {

        /**
         * 目标主机和ProxyRequest或List<ProxyRequest>对应关系
         */
        private final Map<InetSocketAddress, DatagramProxyRequest> requestMap = new HashMap<>(64);

        /**
         * 等待中的DatagramProxyRequest，防止在发送到同一个目标服务器的情况下，接收到的UDP包混淆
         */
        private final Map<InetSocketAddress, Queue<DatagramProxyRequest>> waitRequestMap = new HashMap<>(64);

        private UDPHandler() {
            super(false);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
            InetSocketAddress sender = msg.sender();
            if(!requestMap.containsKey(sender)) {
                if(log.isWarnEnabled())
                    log.warn("UDP Datagram sender from {} can not found.", sender.toString());
                return;
            }

            if(log.isTraceEnabled())
                log.trace("Receive UDP Datagram from {}", sender);

            DatagramProxyRequest obj = requestMap.get(sender);
            writeDatagramToClient(ctx.channel(), msg, obj);
        }

        private void writeDatagramToClient(Channel channel, DatagramPacket packet, DatagramProxyRequest request) {
            InetSocketAddress sender = packet.sender();
            ByteBuf buf = packet.content();
            ByteBuf head = Unpooled.buffer(10);
            head.writeInt(0x01);
            head.writeInt(BaseUtils.parseIPv4StringToInteger(request.getHost()));
            head.writeShort(request.getPort());

            CompositeByteBuf cbuf = Unpooled.compositeBuffer();
            cbuf.addComponent(true, head);
            cbuf.addComponent(true, buf);

            DatagramPacket dp = new DatagramPacket(cbuf, request.senderAddress());
            request.clientChannel().writeAndFlush(dp);

            requestMap.remove(sender);

            //处理等待的Request
            Queue<DatagramProxyRequest> queue = waitRequestMap.get(sender);
            if(queue == null) {
                return;
            } else if(queue.isEmpty()) {
                waitRequestMap.remove(sender);
            }

            channel.writeAndFlush(queue.poll());
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            if(msg instanceof DatagramProxyRequest) {
                DatagramProxyRequest request = (DatagramProxyRequest) msg;
                InetSocketAddress remote = new InetSocketAddress(request.getHost(), request.getPort());

                if(requestMap.containsKey(remote)) {
                    Queue<DatagramProxyRequest> queue = waitRequestMap.get(remote);
                    if(queue == null) {
                        LinkedList<DatagramProxyRequest> l = new LinkedList<>();
                        l.add(request);
                        waitRequestMap.put(remote, l);
                    } else {
                        queue.offer(request);
                    }

                    return;
                }

                requestMap.put(remote, request);
                DatagramPacket packet = new DatagramPacket(request.takeClientMessage(), remote);
                ctx.writeAndFlush(packet);

                if(log.isTraceEnabled())
                    log.trace("Datagram has send to {}:{}", request.getHost(), request.getPort());

            } else {
                log.warn("Unsupport message type: {}", msg.getClass().getName());
            }
        }
    }
}
