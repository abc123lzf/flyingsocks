/*
 * Copyright (c) 2019 abc123lzf <abc123lzf@126.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lzf.flyingsocks.client.proxy.http;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.proxy.ProxyComponent;
import com.lzf.flyingsocks.client.proxy.ProxyRequest;
import com.lzf.flyingsocks.client.proxy.util.MessageDelivererCancelledException;
import com.lzf.flyingsocks.util.BaseUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.IntegerValidator;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import static com.lzf.flyingsocks.client.proxy.ProxyRequest.Protocol;

/**
 * HTTP代理组件
 *
 * @author lzf abc123lzf@126.com
 * @since 2020/12/25 18:51
 */
public class HttpReceiverComponent extends AbstractComponent<ProxyComponent> {

    private static final String FIRST_HTTP_DECODER = "FirstHttpRequestDecoder";

    private static final String FIRST_HTTP_ENCODER = "FirstHttpResponseEncoder";

    private static final String FIRST_HTTP_REQUEST_HANDLER = "FirstHttpRequestHandler";

    private static final String TUNNEL_PROXY_HANDLER = "TunnelProxyHandler";

    private static final HttpResponseStatus RESPONSE_CONNECTION_ESTABLISHED = HttpResponseStatus.valueOf(200, "Connection Established");

    private volatile AuthenticationStrategy authenticationStrategy;

    private volatile EventLoopGroup eventLoopGroup;

    public HttpReceiverComponent(ProxyComponent parent) {
        super("HttpRequestReceiver", Objects.requireNonNull(parent));
    }

    @Override
    protected void initInternal() {
        ConfigManager<?> configManager = getConfigManager();
        HttpProxyConfig config = configManager.getConfig(HttpProxyConfig.NAME, HttpProxyConfig.class);
        if (config.isAuth()) {
            this.authenticationStrategy = new SimpleAuthenticationStrategy(
                    config.getUsername(), config.getPassword());
        }

        configManager.registerConfigEventListener(event -> {
            if (!(event.getSource() instanceof HttpProxyConfig) ||
                    !Objects.equals(event.getEvent(), Config.UPDATE_EVENT)) {
                return;
            }

            HttpProxyConfig cfg = (HttpProxyConfig) event.getSource();
            if (cfg.isAuth()) {
                this.authenticationStrategy = new SimpleAuthenticationStrategy(
                        config.getUsername(), config.getPassword());
            }
        });

        int threadCount;
        int cpus = configManager.availableProcessors();
        if (cpus <= 4) {
            threadCount = 2;
        } else if (cpus <= 16) {
            threadCount = 4;
        } else {
            threadCount = 6;
        }
        this.eventLoopGroup = parent.createNioEventLoopGroup(threadCount);
        super.initInternal();
    }

    @Override
    protected void startInternal() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(this.eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(FIRST_HTTP_DECODER, new HttpRequestDecoder());
                        pipeline.addLast(FIRST_HTTP_ENCODER, new HttpResponseEncoder());
                        pipeline.addLast(FIRST_HTTP_REQUEST_HANDLER, new HttpProxyRequestHandler());
                    }
                });

        HttpProxyConfig config = getConfigManager().getConfig(HttpProxyConfig.NAME, HttpProxyConfig.class);
        int port = config.getBindPort();
        String address = config.getBindAddress();

        bootstrap.bind(address, port).addListener((ChannelFuture future) -> {
            if (!future.isSuccess()) {
                log.error("HTTP Proxy service bind error", future.cause());
            } else {
                log.info("Bind port {} success", port);
            }
        }).awaitUninterruptibly();

        super.startInternal();
    }

    @Override
    protected void stopInternal() {
        EventLoopGroup eventLoopGroup = this.eventLoopGroup;
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
        super.stopInternal();
    }


    private static void writeFailureResponse(ChannelHandlerContext ctx, DefaultFullHttpResponse response) {
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    private final class HttpProxyRequestHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof HttpRequest) {
                HttpRequest request = (HttpRequest) msg;
                processFirstRequest(ctx, request);
                return;
            }

            ctx.fireChannelRead(msg);
        }

        private void processFirstRequest(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
            AuthenticationStrategy strategy = HttpReceiverComponent.this.authenticationStrategy;
            if (strategy != null) {
                String authorization = request.headers().get(HttpHeaderNames.PROXY_AUTHORIZATION);
                if (StringUtils.isBlank(authorization)) {
                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED);
                    response.headers().add(HttpHeaderNames.PROXY_AUTHENTICATE, strategy.proxyAuthenticateHeader());
                    writeFailureResponse(ctx, response);
                    return;
                }

                String[] arr = StringUtils.split(authorization, ' ');
                if (arr.length != 2) {
                    writeFailureResponse(ctx, new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.BAD_REQUEST));
                    return;
                }

                if (!strategy.grantAuthorization(arr[0], arr[1])) {
                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED);
                    response.headers().add(HttpHeaderNames.PROXY_AUTHENTICATE, strategy.proxyAuthenticateHeader());
                    writeFailureResponse(ctx, response);
                    return;
                } else {
                    request.headers().remove(HttpHeaderNames.PROXY_AUTHORIZATION);
                    strategy.afterAuthorizationSuccess(ctx.pipeline(), arr[0], arr[1]);
                }
            }

            if (request.method() == HttpMethod.CONNECT) {
                processConnectRequest(ctx, request);
                return;
            }

            processOtherMethodRequest(ctx, request);
        }


        private void processConnectRequest(ChannelHandlerContext ctx, HttpRequest request) {
            String[] str = StringUtils.split(request.uri(), ':');
            if (str.length != 2 || !validateTargetHost(str[0]) || !IntegerValidator.getInstance().isValid(str[1])) {
                throw new IllegalArgumentException("Illegal domain:" + request.uri());
            }

            ProxyRequest pr = new ProxyRequest(str[0], Integer.parseInt(str[1]), ctx.channel(), Protocol.TCP);
            getParentComponent().publish(pr);

            ChannelPipeline pipeline = ctx.pipeline();
            ctx.write(new DefaultFullHttpResponse(request.protocolVersion(), RESPONSE_CONNECTION_ESTABLISHED));
            pipeline.addLast(TUNNEL_PROXY_HANDLER, new TunnelProxyHandler(pr));
            ctx.flush();
        }


        private void processOtherMethodRequest(ChannelHandlerContext ctx, HttpRequest request) throws IOException {
            URL url = new URL(request.uri());
            String host = url.getHost();
            if (!validateTargetHost(host)) {
                log.warn("Illegal target url: {}", url);
                ctx.close();
            }

            int port = url.getPort();
            if (port == -1) {
                port = 80;
            }

            if (!BaseUtils.isPort(port)) {
                log.warn("Illegal target url: {}", url);
                ctx.close();
            }

            ProxyRequest pr = new ProxyRequest(host, port, ctx.channel(), Protocol.TCP);
            ctx.pipeline().addLast(new PlaintextProxyHandler(pr, request));
            getParentComponent().publish(pr);
        }


        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("Exception at HttpProxyRequestHandler");
            if (cause instanceof MalformedURLException) {
                ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST), ctx.voidPromise());
                ctx.close();
            } else {
                ctx.fireExceptionCaught(cause);
            }
        }
    }

    /**
     * 实现HTTP隧道代理
     * 主要用于HTTPS协议的网站
     */
    private static final class TunnelProxyHandler extends ChannelInboundHandlerAdapter {

        private final ProxyRequest proxyRequest;

        TunnelProxyHandler(ProxyRequest proxyRequest) {
            this.proxyRequest = Objects.requireNonNull(proxyRequest);
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            ChannelPipeline cp = ctx.pipeline();
            cp.remove(FIRST_HTTP_REQUEST_HANDLER);
            cp.remove(FIRST_HTTP_DECODER);
            cp.remove(FIRST_HTTP_ENCODER);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                try {
                    proxyRequest.transferClientMessage(buf);
                } finally {
                    buf.release();
                }
                return;
            }

            ctx.fireChannelRead(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof MessageDelivererCancelledException) {
                proxyRequest.close();
                ctx.close();
                return;
            }

            ctx.fireExceptionCaught(cause);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            proxyRequest.close();
            ctx.fireChannelInactive();
        }
    }


    private static boolean validateTargetHost(String host) {
        return BaseUtils.isHostName(host) || BaseUtils.isIPv4Address(host);
    }

    /**
     * 实现常规代理
     * 主要用于非HTTPS网站代理
     */
    private final class PlaintextProxyHandler extends ChannelInboundHandlerAdapter {

        private final EmbeddedChannel requestEncoder;

        private final ProxyRequest proxyRequest;

        public PlaintextProxyHandler(ProxyRequest proxyRequest, HttpRequest firstRequest) throws IOException {
            this.proxyRequest = Objects.requireNonNull(proxyRequest);
            this.requestEncoder = new EmbeddedChannel(new HttpRequestEncoder() {
                @Override
                protected boolean isContentAlwaysEmpty(HttpRequest msg) {
                    return true;
                }
            });


            requestEncoder.writeOutbound(firstRequest);
            ByteBuf result = requestEncoder.readOutbound();
            requestEncoder.writeOutbound(LastHttpContent.EMPTY_LAST_CONTENT);
            try {
                proxyRequest.transferClientMessage(result);
            } finally {
                ReferenceCountUtil.release(result);
            }
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            ChannelPipeline pipeline = ctx.pipeline();
            pipeline.remove(FIRST_HTTP_ENCODER);
            pipeline.remove(FIRST_HTTP_REQUEST_HANDLER);
            pipeline.addFirst(new IdleStateHandler(0, 0, 8));
            pipeline.addFirst(new HttpRequestDecoder());
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                ctx.close();
                return;
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg == LastHttpContent.EMPTY_LAST_CONTENT) {
                return;
            }

            if (msg instanceof HttpRequest) {
                HttpRequest request = (HttpRequest) msg;

                HttpHeaders header = request.headers();
                if (header.contains(HttpHeaderNames.PROXY_CONNECTION)) {
                    String val = header.get(HttpHeaderNames.PROXY_CONNECTION);
                    header.remove(HttpHeaderNames.PROXY_CONNECTION).add(HttpHeaderNames.CONNECTION, val);
                }

                URL url = new URL(request.uri());
                if (!url.getHost().equals(proxyRequest.getHost())) {
                    log.warn("Reuse Http proxy connection");
                    ctx.close();
                    return;
                }

                request.setUri(url.getPath());
                requestEncoder.writeOutbound(msg);
                ByteBuf buf = requestEncoder.readOutbound();
                requestEncoder.writeOutbound(LastHttpContent.EMPTY_LAST_CONTENT);
                try {
                    proxyRequest.transferClientMessage(buf);
                } finally {
                    ReferenceCountUtil.release(buf);
                }
            } else if (msg instanceof HttpContent) {
                try {
                    proxyRequest.transferClientMessage(((ByteBufHolder) msg).content());
                } finally {
                    ReferenceCountUtil.release(msg);
                }
            } else {
                ctx.fireChannelRead(msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof MalformedURLException) {
                log.info("Illegal URI field.", cause);
                ctx.close();
                return;
            }

            log.error("An error occur in PlaintextProxyHandler", cause);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            requestEncoder.close();
            proxyRequest.close();
            ctx.fireChannelInactive();
        }
    }
}
