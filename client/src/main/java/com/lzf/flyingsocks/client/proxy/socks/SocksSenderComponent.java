package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.AbstractComponent;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.concurrent.*;

public final class SocksSenderComponent extends AbstractComponent<SocksProxyComponent> {

    private ExecutorService requestQueuePoller;

    private Bootstrap connectBootstrap;

    SocksSenderComponent(SocksProxyComponent proxyComponent) {
        super("SocksSender", proxyComponent);
    }

    @Override
    protected void initInternal() {
        requestQueuePoller = SocksProxyComponent.constructSingleExecutor();

        connectBootstrap = new Bootstrap();
        connectBootstrap.group(getParentComponent().getWorkerEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.AUTO_CLOSE, true);
    }

    @Override
    protected void startInternal() {
        requestQueuePoller.submit(() -> {
           while (true) {
               try {
                   SocksProxyRequest request = getParentComponent().pollProxyRequest(false);

                   String host = request.getHost();
                   int port = request.getPort();

                   if(log.isTraceEnabled())
                       log.trace("connect to server {}:{} established...", host, port);

                   Bootstrap b = connectBootstrap.clone();
                   b.handler(new ChannelInitializer<SocketChannel>() {
                       @Override
                       protected void initChannel(SocketChannel ch) {
                           ch.pipeline().addFirst(new DirectConnectHandler(request));
                       }
                   });

                   b.connect(host, port).addListener(new ConnectListener(request));

               } catch (InterruptedException e) {
                   return;
               }
           }
        });
    }

    @Override
    protected void stopInternal() {
        requestQueuePoller.shutdown();
    }

    /**
     * 连接监听器
     */
    private final class ConnectListener implements ChannelFutureListener {
        private final SocksProxyRequest request;

        private ConnectListener(SocksProxyRequest request) {
            this.request = request;
        }

        @Override
        public void operationComplete(ChannelFuture channelFuture) {
            if(!channelFuture.isSuccess()){
                if(log.isWarnEnabled())
                    log.warn("connect establish failure, from " + request.getHost() + ":" + request.getPort(), channelFuture.cause());
                request.getClientChannel().close();
                channelFuture.channel().close();
            } else {
                if(log.isTraceEnabled())
                    log.trace("connect establish success, from {}:{}", request.getHost(), request.getPort());
                request.setServerChannel(channelFuture.channel());
            }

            channelFuture.removeListener(this);
        }
    }

    /**
     * 与目标服务器直连的进站处理器，一般用于无需代理的网站
     */
    private final class DirectConnectHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final SocksProxyRequest request;

        private DirectConnectHandler(SocksProxyRequest request) {
            super(false);
            this.request = request;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            if (log.isTraceEnabled())
                log.trace("Channel Active from {}:{}", request.getHost(), request.getPort());

            Channel sc = ctx.channel();
            request.setServerChannel(sc);

        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            request.getClientChannel().writeAndFlush(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(log.isWarnEnabled())
                log.warn("Exception caught in Proxy Handler", cause);

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

    }

}
