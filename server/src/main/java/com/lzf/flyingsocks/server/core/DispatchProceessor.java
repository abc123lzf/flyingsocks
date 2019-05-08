package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.protocol.ProxyRequestMessage;
import com.lzf.flyingsocks.protocol.ProxyResponseMessage;
import com.lzf.flyingsocks.util.ReturnableLinkedHashSet;
import com.lzf.flyingsocks.util.ReturnableSet;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.*;
import java.util.concurrent.*;

public class DispatchProceessor extends AbstractComponent<ProxyProcessor> {

    private final Bootstrap bootstrap;

    private ExecutorService requestReceiver;

    public DispatchProceessor(ProxyProcessor parent) {
        super("DispatcherProcessor", parent);
        this.bootstrap = initBootstrap();
    }

    @Override
    protected void initInternal() {
        requestReceiver = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
        requestReceiver.submit(new DispatcherTask());

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

    private Bootstrap initBootstrap() {
        return new Bootstrap().group(getParentComponent().getRequestProcessWorker())
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000)
            .option(ChannelOption.SO_KEEPALIVE, true);
    }

    /**
     * 实现连接复用，可以预防客户端代理消息半包的现象
     */
    static final class ActiveConnection {
        final String host;          //目标主机IP/域名
        final int port;             //目标主机端口号
        final String clientId;      //客户端的客户端的ChannelID
        ChannelFuture future;       //该连接的ChannelFuture
        final Queue<ByteBuf> msgQueue;    //若上述future持有的Channel尚未Active，则该队列负责保存该连接的客户端数据

        ActiveConnection(String host, int port, String clientId) {
            this.host = host;
            this.port = port;
            this.clientId = clientId;
            msgQueue = new LinkedList<>();
        }

        @Override
        public int hashCode() {
            return host.hashCode() ^ port ^ clientId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj instanceof ActiveConnection) {
                ActiveConnection c = (ActiveConnection) obj;
                return this.host.equals(c.host) && this.port == c.port && this.clientId.equals(c.clientId);
            }
            return false;
        }
    }

    private final class DispatcherTask implements Runnable {
        private final Map<ClientSession, ReturnableSet<ActiveConnection>> activeConnectionMap = new LinkedHashMap<>(64);

        @Override
        public void run() {
            try {
                Thread thread = Thread.currentThread();
                while (!thread.isInterrupted()) {
                    ProxyTask task = getParentComponent().pollProxyTask();
                    try {
                        ProxyRequestMessage prm = task.getProxyRequestMessage();
                        ClientSession cs = task.getSession();

                        if(!cs.isActive())
                            continue;

                        ReturnableSet<ActiveConnection> set = activeConnectionMap.computeIfAbsent(cs, key -> new ReturnableLinkedHashSet<>(128));

                        ActiveConnection conn = new ActiveConnection(prm.getHost(), prm.getPort(), prm.getChannelId());
                        ActiveConnection sconn;
                        if((sconn = set.getIfContains(conn)) != null)
                            conn = sconn;
                        if(sconn != null) {

                            ChannelFuture f = conn.future;
                            Channel c = f.channel();
                            if(f.isDone() && f.isSuccess()) { //如果连接成功
                                if (c.isActive()) { //如果连接仍处于活跃状态
                                    ByteBuf buf;
                                    while((buf = conn.msgQueue.poll()) != null) {
                                        c.write(buf);
                                    }
                                    c.writeAndFlush(prm.getMessage());
                                    if(!c.isActive()) //再次检查连接是否活跃
                                        set.remove(conn);
                                } else { //如果连接失效
                                    set.remove(conn);
                                }
                            } else if(!f.isDone()) { //如果正处于连接状态
                                conn.msgQueue.add(prm.getMessage());
                            } else { //如果连接建立失败
                                set.remove(conn);
                            }
                        } else {
                            Bootstrap b = bootstrap.clone();
                            b.handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel ch) {
                                    ch.pipeline().addLast(new DispatchHandler(task));
                                }
                            });

                            conn.future = b.connect(prm.getHost(), prm.getPort()).addListener(future -> {
                                if (!future.isSuccess()) {
                                    ProxyResponseMessage resp = new ProxyResponseMessage(prm.getChannelId());
                                    resp.setState(ProxyResponseMessage.State.FAILURE);
                                    try {
                                        cs.writeAndFlushMessage(resp.serialize());
                                    } catch (IllegalStateException e) {
                                        if (log.isTraceEnabled())
                                            log.trace("Client from {} has disconnect.", cs.remoteAddress().getAddress());
                                    }
                                }
                            });

                            set.add(conn);
                        }

                        clearUnactiveConnection();

                    } catch (Exception e) {
                        if(log.isWarnEnabled())
                            log.warn("Exception occur, at RequestReceiver thread", e);
                    }
                }
            } catch (InterruptedException e) {
                if (log.isInfoEnabled())
                    log.info("RequestReceiver interrupt, from {}", getName());
            }
        }

        /**
         * 清除废弃的ActiveConnection对象
         */
        private void clearUnactiveConnection() {
            Iterator<Map.Entry<ClientSession, ReturnableSet<ActiveConnection>>> it = activeConnectionMap.entrySet().iterator();
            it.forEachRemaining(e -> {
                if(!e.getKey().isActive())
                    it.remove();
                else {
                    Iterator<ActiveConnection> sit = e.getValue().iterator();
                    sit.forEachRemaining(ac -> {
                        if (ac.future.isDone() && !ac.future.channel().isActive())
                            sit.remove();
                    });
                }
            });
        }
    }

    /**
     * 将来自客户端的请求内容转发给目标服务器
     */
    private static class DispatchHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final ProxyTask proxyTask;

        private DispatchHandler(ProxyTask task) {
            super(false);
            this.proxyTask = task;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ctx.writeAndFlush(proxyTask.getProxyRequestMessage().getMessage());
            super.channelActive(ctx);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            ProxyResponseMessage prm = new ProxyResponseMessage(proxyTask.getProxyRequestMessage().getChannelId());
            prm.setState(ProxyResponseMessage.State.SUCCESS);
            prm.setMessage(msg);

            proxyTask.getSession().writeAndFlushMessage(prm.serialize());
        }
    }
}
