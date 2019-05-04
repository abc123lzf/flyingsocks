package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.protocol.ProxyRequestMessage;
import com.lzf.flyingsocks.protocol.ProxyResponseMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

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
        requestReceiver.submit(() -> {
            try {
                Thread thread = Thread.currentThread();
                while (!thread.isInterrupted()) {
                    ProxyTask task = getParentComponent().pollProxyTask();
                    try {
                        Bootstrap b = bootstrap.clone();
                        ProxyRequestMessage prm = task.getProxyRequestMessage();

                        b.handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(new DispatchHandler(task));
                            }
                        });

                        b.connect(prm.getHost(), prm.getPort()).addListener(future -> {
                            if (!future.isSuccess()) {
                                ClientSession s = task.getSession();
                                ProxyResponseMessage resp = new ProxyResponseMessage(prm.getChannelId());
                                resp.setState(ProxyResponseMessage.State.FAILURE);
                                try {
                                    s.writeAndFlushMessage(resp.serialize());
                                } catch (IllegalStateException e) {
                                    if(log.isTraceEnabled())
                                        log.trace("Client from {} has disconnect.", s.remoteAddress().getAddress());
                                }
                            }
                        });
                    } catch (Exception e) {
                        if(log.isWarnEnabled())
                            log.warn("Exception occur, at RequestReceiver thread", e);
                    }
                }
            } catch (InterruptedException e) {
                if (log.isInfoEnabled())
                    log.info("RequestReceiver interrupt, from {}", getName());
            }

            return null;
        });

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
