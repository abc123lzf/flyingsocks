package com.lzf.flyingsocks.client.proxy.direct;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.client.proxy.ProxyComponent;
import com.lzf.flyingsocks.client.proxy.ProxyRequest;
import com.lzf.flyingsocks.client.proxy.ProxyRequestSubscriber;
import com.lzf.flyingsocks.client.proxy.util.MessageReceiver;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * @author lzf abc123lzf@126.com
 * @since 2020/12/16 7:08
 */
public class DirectForwardComponent extends AbstractComponent<ProxyComponent> implements ProxyRequestSubscriber {

    /**
     * Bootstrap模板
     */
    private final Bootstrap connectTemplateBootstrap;

    public DirectForwardComponent(ProxyComponent component) {
        super("DirectForwardComponent", component);

        Bootstrap template = new Bootstrap();
        template.group(parent.createNioEventLoopGroup(4))
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.AUTO_CLOSE, true);

        this.connectTemplateBootstrap = template;
    }


    @Override
    protected void startInternal() {
        parent.registerSubscriber(this);
        super.startInternal();
    }

    @Override
    public void receive(ProxyRequest request) {
        final String host = request.getHost();
        final int port = request.getPort();

        log.trace("connect to server {}:{} established...", host, port);

        Bootstrap b = connectTemplateBootstrap.clone();
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addFirst(new TCPConnectHandler(request));
            }
        });

        b.connect(host, port).addListener((ChannelFuture f) -> {
            if (!f.isSuccess()) {
                handleConnectException(request, f.cause());
                f.channel().close();
                request.close();
            }

            log.trace("connect establish success, target server {}:{}", host, port);
        });
    }


    private void handleConnectException(ProxyRequest request, Throwable throwable) {
        if (throwable instanceof ConnectTimeoutException) {
            if (log.isInfoEnabled())
                log.info("connect to " + request.getHost() + ":" + request.getPort() + " failure, cause connect timeout");
        } else if (throwable instanceof UnknownHostException) {
            if (log.isWarnEnabled())
                log.warn("Connect failure: Unknow domain {}", request.getHost());
        } else {
            if (log.isWarnEnabled())
                log.warn("connect establish failure, from " + request.getHost() + ":" + request.getPort(), throwable);
        }

        request.closeClientChannel();
    }


    @Override
    public boolean receiveNeedlessProxy() {
        return true;
    }

    @Override
    public Set<ProxyRequest.Protocol> requestProtocol() {
        return ProxyRequestSubscriber.ONLY_TCP;
    }


    /**
     * 与目标服务器直连的进站处理器，一般用于无需代理的网站
     */
    private final class TCPConnectHandler extends SimpleChannelInboundHandler<ByteBuf> {
        final ProxyRequest request;

        private TCPConnectHandler(ProxyRequest request) {
            super(false);
            this.request = request;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws IOException {
            if (log.isTraceEnabled()) {
                log.trace("Channel Active from {}:{}", request.getHost(), request.getPort());
            }

            request.setClientMessageReceiver(new MessageReceiver() {
                @Override
                public void receive(ByteBuf message) {
                    ctx.channel().writeAndFlush(message);
                }

                @Override
                public void close() {
                    Channel channel = ctx.channel();
                    if (channel.isOpen()) {
                        channel.close();
                    }
                }
            });

            ctx.fireChannelActive();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof IOException) {
                log.trace("Direct TCP Connection force to close, from remote server {}:{}", request.getHost(), request.getPort());
            }

            log.warn("Exception caught in TCP ProxyHandler", cause);
            request.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.trace("connection close, from {}:{}", request.getHost(), request.getPort());

            ctx.pipeline().remove(this);
            request.close();

            ctx.fireChannelInactive();
            ctx.close();
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            request.clientChannel().writeAndFlush(msg);
        }
    }

}
