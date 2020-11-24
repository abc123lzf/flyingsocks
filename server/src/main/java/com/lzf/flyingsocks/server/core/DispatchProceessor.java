package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.protocol.ProxyRequestMessage;
import com.lzf.flyingsocks.protocol.ProxyResponseMessage;
import com.lzf.flyingsocks.protocol.SerializationException;
import com.lzf.flyingsocks.util.ReturnableLinkedHashSet;
import com.lzf.flyingsocks.util.ReturnableSet;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class DispatchProceessor extends AbstractComponent<ProxyProcessor> {

    /**
     * 对于长时间没有通信的活跃TCP连接/UDP通信端口，当超出这个时间时关闭连接
     */
    private static final long DEFAULT_TIMEOUT = 60 * 1000L;

    /**
     * TCP客户端连接引导模板
     */
    private final Bootstrap tcpBootstrap;

    /**
     * UDP引导模板
     */
    private final Bootstrap udpBootstrap;

    /**
     * 该模块核心线程池
     */
    private ExecutorService requestReceiver;

    DispatchProceessor(ProxyProcessor parent) {
        super("DispatcherProcessor", parent);
        this.tcpBootstrap = new Bootstrap().group(parent.getRequestProcessWorker())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        this.udpBootstrap = new Bootstrap().group(parent.getRequestProcessWorker())
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }

    @Override
    protected void initInternal() {
        requestReceiver = new ThreadPoolExecutor(0, 4, 30, TimeUnit.SECONDS,
                new SynchronousQueue<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());

        int cpus = Runtime.getRuntime().availableProcessors();
        int task;
        if (cpus <= 2) {
            task = 2;
        } else if (cpus <= 6) {
            task = 3;
        } else {
            task = 4;
        }

        log.info("Using {} dispatcher thread.", task);

        for (int i = 0; i < task; i++) {
            requestReceiver.submit(new DispatcherTask());
        }

        super.initInternal();
    }

    @Override
    protected void startInternal() {
        super.startInternal();
    }

    @Override
    protected void stopInternal() {
        requestReceiver.shutdownNow();
        super.stopInternal();
    }


    /**
     * 实现连接复用，可以预防客户端代理消息半包的现象
     */
    static final class ActiveConnection {
        final String host;              //目标主机IP/域名
        final int port;                 //目标主机端口号
        final int clientSerialId;       //客户端事务ID
        ChannelFuture future;           //该连接的ChannelFuture
        final Queue<ByteBuf> msgQueue;  //若上述future持有的Channel尚未Active，则该队列负责保存该连接的客户端数据
        long lastActiveTime;            //最近一次的数据发送/接收时间，对长时间无数据发送、接收的连接采取关闭策略

        ActiveConnection(String host, int port, int clientSerialId) {
            this.host = host;
            this.port = port;
            this.clientSerialId = clientSerialId;
            msgQueue = new LinkedList<>();
            lastActiveTime = System.currentTimeMillis();
        }

        @Override
        public int hashCode() {
            return host.hashCode() ^ (port << 16) ^ clientSerialId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof ActiveConnection) {
                ActiveConnection c = (ActiveConnection) obj;
                return this.host.equals(c.host) && this.port == c.port && this.clientSerialId == c.clientSerialId;
            }

            return false;
        }
    }

    private final class DispatcherTask implements Runnable, ProxyTaskSubscriber {
        private final Map<ClientSession, ReturnableSet<ActiveConnection>> activeConnectionMap = new LinkedHashMap<>(64);
        private final BlockingQueue<ProxyTask> taskQueue = new LinkedBlockingQueue<>();

        private DispatcherTask() {
            parent.registerSubscriber(this);
        }

        @Override
        public void run() {
            try {
                final Thread thread = Thread.currentThread();
                while (!thread.isInterrupted()) {
                    final ProxyTask task = taskQueue.poll(5, TimeUnit.MILLISECONDS);
                    if (task == null) {
                        checkoutConnection();
                        continue;
                    }

                    if (log.isTraceEnabled())
                        log.trace("Receive ProxyTask at DispatcherTask thread");

                    try {
                        final ProxyRequestMessage prm = task.getRequestMessage();
                        final ClientSession cs = task.session();

                        if (!cs.isActive())
                            continue;

                        final ReturnableSet<ActiveConnection> set = activeConnectionMap.computeIfAbsent(cs,
                                key -> new ReturnableLinkedHashSet<>(128));

                        final String host = prm.getHost();
                        final int port = prm.getPort();

                        ActiveConnection conn = new ActiveConnection(host, port, prm.serialId());
                        ActiveConnection sconn;
                        if ((sconn = set.getIfContains(conn)) != null) {
                            conn = sconn;
                        }
                        if (sconn != null) {
                            if (prm.getProtocol() == ProxyRequestMessage.Protocol.CLOSE) {
                                conn.future.channel().close();
                                set.remove(conn);
                                continue;
                            }

                            ChannelFuture f = conn.future;
                            Channel c = f.channel();
                            if (f.isDone() && f.isSuccess()) { //如果连接成功
                                if (c.isActive()) { //如果连接仍处于活跃状态
                                    ByteBuf buf;
                                    while ((buf = conn.msgQueue.poll()) != null) {  //优先处理消息队列中的消息,保证其顺序
                                        c.write(buf);
                                    }
                                    c.writeAndFlush(prm.getMessage());
                                    conn.lastActiveTime = System.currentTimeMillis();
                                }
                            } else if (!f.isDone()) { //如果正处于连接状态
                                conn.msgQueue.add(prm.getMessage());
                            } else { //如果连接建立失败
                                set.remove(conn);
                            }
                        } else {
                            switch (prm.getProtocol()) {
                                case TCP: {
                                    Bootstrap b = tcpBootstrap.clone();
                                    b.handler(new TCPDispatchHandler(task));
                                    conn.future = b.connect(host, port).addListener(future -> {
                                        if (!future.isSuccess()) { //如果连接没有建立成功，那么向客户端返回一个错误的消息
                                            log.warn("Can not connect to {}:{}", host, port);
                                            writeFailureResponse(cs, prm);
                                        } else {
                                            log.trace("Connect to {}:{} success", host, port);
                                        }
                                    });
                                }
                                break;

                                case UDP: {
                                    Bootstrap b = udpBootstrap.clone();
                                    b.handler(new UDPDisaptchHandler(task));
                                    conn.future = b.bind(0).addListener(future -> {
                                        if (!future.isSuccess()) {
                                            log.warn("Can not bind UDP Port");
                                            writeFailureResponse(cs, prm);
                                        } else {
                                            log.trace("Bind UDP Port success, ready to send packet to {}:{}", host, port);
                                        }
                                    });
                                }
                                break;
                            }

                            set.add(conn);
                        }

                        checkoutConnection();
                    } catch (Exception e) {
                        if (log.isWarnEnabled())
                            log.warn("Exception occur, at RequestReceiver thread", e);
                    }
                }
            } catch (InterruptedException e) {
                if (log.isInfoEnabled())
                    log.info("RequestReceiver interrupt, from {}", getName());
            } finally {
                parent.removeSubscriber(this);
            }
        }

        @Override
        public void receive(ProxyTask task) {
            taskQueue.add(task);
        }


        private void writeFailureResponse(ClientSession session, ProxyRequestMessage request) {
            ProxyResponseMessage resp = new ProxyResponseMessage(request.serialId());
            resp.setState(ProxyResponseMessage.State.FAILURE);
            try {
                session.writeAndFlushMessage(resp.serialize());
            } catch (SerializationException e) {  //不应该发生
                log.error("Serialize FailureResponse occur a exception", e);
            } catch (IllegalStateException e) {
                if (log.isTraceEnabled())
                    log.trace("Client from {} has disconnect.", session.remoteAddress().getAddress());
            }
        }

        /**
         * 检查ActiveConnection对象
         */
        private void checkoutConnection() {
            Iterator<Map.Entry<ClientSession, ReturnableSet<ActiveConnection>>> it = activeConnectionMap.entrySet().iterator();
            it.forEachRemaining(e -> {
                final ClientSession cs = e.getKey();
                final ReturnableSet<ActiveConnection> val = e.getValue();

                if (!cs.isActive()) {
                    it.remove();
                    //清除连接中断的客户端中所有ActiveConnection的msgQueue队列中的ByteBuf对象
                    val.forEach(ac -> {
                        if (!ac.msgQueue.isEmpty())
                            ac.msgQueue.forEach(ByteBuf::release);
                    });
                } else {
                    long now = System.currentTimeMillis();
                    Iterator<ActiveConnection> sit = val.iterator();
                    sit.forEachRemaining(ac -> {
                        if (ac.future.isDone()) {
                            //如果已经建立过连接但是该连接已经不活跃了，那么清除这个ActiveConnection
                            if (!ac.future.channel().isActive()) {
                                if (!ac.msgQueue.isEmpty())
                                    ac.msgQueue.forEach(ByteBuf::release);
                                sit.remove();
                            } else {
                                Channel ch = ac.future.channel();
                                if (ac.msgQueue.isEmpty() && now - ac.lastActiveTime > DEFAULT_TIMEOUT) {
                                    ch.close();
                                    sit.remove();
                                } else {
                                    if (ch instanceof SocketChannel) {
                                        ByteBuf buf;
                                        while ((buf = ac.msgQueue.poll()) != null)
                                            ch.write(buf);
                                        ch.flush();
                                        ac.lastActiveTime = now;
                                    } else if (ch instanceof DatagramChannel) {
                                        InetSocketAddress addr = new InetSocketAddress(ac.host, ac.port);
                                        ByteBuf buf;
                                        while ((buf = ac.msgQueue.poll()) != null)
                                            ch.write(new DatagramPacket(buf, addr));
                                        ch.flush();
                                        ac.lastActiveTime = now;
                                    } else {
                                        log.error("Unsupport Channel type: {}", ch.getClass().getName());
                                    }
                                }
                            }
                        }
                    });
                }
            });
        }
    }

    /**
     * 将来自客户端的TCP协议请求内容转发给目标服务器
     */
    private class TCPDispatchHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final ProxyTask proxyTask;
        private final String host;
        private final int port;

        private TCPDispatchHandler(ProxyTask task) {
            super(false);
            this.proxyTask = task;
            this.host = proxyTask.getRequestMessage().getHost();
            this.port = proxyTask.getRequestMessage().getPort();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof IOException) {
                log.info("Target remote host {}:{} close, cause IOException", host, port);
            } else {
                log.warn("DispathcerHandelr occur a exception", cause);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.trace("Connection close by {}:{}", host, port);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ByteBuf buf = proxyTask.getRequestMessage().getMessage();
            ctx.writeAndFlush(buf);
            super.channelActive(ctx);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            log.trace("Receive from {}:{} response.", host, port);

            ProxyResponseMessage prm = new ProxyResponseMessage(proxyTask.getRequestMessage().serialId());
            prm.setState(ProxyResponseMessage.State.SUCCESS);
            prm.setMessage(msg);
            try {
                proxyTask.session().writeAndFlushMessage(prm.serialize());
            } catch (IllegalStateException e) {
                ctx.close();
            } finally {
                msg.release();
            }
        }
    }

    /**
     * 将来自客户端的UDP协议请求内容转发给目标服务器
     */
    private class UDPDisaptchHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        private final ProxyTask proxyTask;
        private final String host;
        private final int port;

        private UDPDisaptchHandler(ProxyTask task) {
            super(false);
            this.proxyTask = task;
            this.host = proxyTask.getRequestMessage().getHost();
            this.port = proxyTask.getRequestMessage().getPort();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ByteBuf buf = proxyTask.getRequestMessage().getMessage();
            DatagramPacket packet = new DatagramPacket(buf, new InetSocketAddress(host, port));
            ctx.writeAndFlush(packet);
            super.channelActive(ctx);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
            log.trace("Receive from {}:{} Datagram.", host, port);

            ProxyResponseMessage prm = new ProxyResponseMessage(proxyTask.getRequestMessage().serialId());
            prm.setState(ProxyResponseMessage.State.SUCCESS);
            prm.setMessage(msg.content());
            try {
                proxyTask.session().writeAndFlushMessage(prm.serialize());
            } catch (IllegalStateException e) {
                ctx.close();
            } finally {
                msg.release();
            }
        }
    }
}
