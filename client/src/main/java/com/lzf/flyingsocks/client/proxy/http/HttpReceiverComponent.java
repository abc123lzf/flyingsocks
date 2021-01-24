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
import com.lzf.flyingsocks.client.proxy.ProxyComponent;
import com.lzf.flyingsocks.client.proxy.ProxyRequest;
import com.lzf.flyingsocks.client.proxy.util.MessageDelivererCancelledException;
import com.lzf.flyingsocks.util.BaseUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.IntegerValidator;

import java.net.URISyntaxException;
import java.util.Objects;

import static com.lzf.flyingsocks.client.proxy.ProxyRequest.Protocol;

/**
 * HTTP/HTTPS代理协议处理
 *
 * @author lzf abc123lzf@126.com
 * @since 2020/12/25 18:51
 */
public class HttpReceiverComponent extends AbstractComponent<ProxyComponent> {

    private static final HttpResponseStatus CONNECTION_ESTABLISHED = HttpResponseStatus.valueOf(200, "Connection Established");

    private volatile AuthenticationStrategy authenticationStrategy;

    public HttpReceiverComponent(ProxyComponent parent) {
        super("HttpRequestReceiver", Objects.requireNonNull(parent));
    }

    @Override
    protected void initInternal() {
        HttpProxyConfig config = getConfigManager().getConfig(HttpProxyConfig.NAME, HttpProxyConfig.class);
        if (config.isAuth()) {
            this.authenticationStrategy = new SimpleAuthenticationStrategy(config.getUsername(), config.getPassword());
        }

        getConfigManager().registerConfigEventListener(event -> {
            if (!(event.getSource() instanceof HttpProxyConfig) || !Objects.equals(event.getEvent(), Config.UPDATE_EVENT)) {
                return;
            }

            HttpProxyConfig cfg = (HttpProxyConfig) event.getSource();
            if (cfg.isAuth()) {
                this.authenticationStrategy = new SimpleAuthenticationStrategy(config.getUsername(), config.getPassword());
            }
        });

        super.initInternal();
    }

    @Override
    protected void startInternal() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(parent.createNioEventLoopGroup(2))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(512 * 1024));
                        pipeline.addLast(new HttpProxyRequestHandler());
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
        super.stopInternal();
    }


    private static void writeFailureResponse(ChannelHandlerContext ctx, DefaultFullHttpResponse response) {
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    private final class HttpProxyRequestHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof FullHttpRequest) {
                FullHttpRequest request = (FullHttpRequest) msg;
                try {
                    channelRead0(ctx, request);
                } catch (Exception e) {
                    writeFailureResponse(ctx, new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.BAD_REQUEST));
                    throw e;
                } finally {
                    ReferenceCountUtil.release(msg);
                }
                return;
            }

            ctx.fireChannelRead(msg);
        }

        private void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            // 处理认证
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
                    strategy.afterAuthorizationSuccess(ctx.pipeline(), arr[0], arr[1]);
                }
            }

            HttpMethod method = request.method();
            if (method == HttpMethod.CONNECT) {
                processConnectMethod(ctx, request);
                return;
            }

            writeFailureResponse(ctx, new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.METHOD_NOT_ALLOWED));
        }

        private void processConnectMethod(ChannelHandlerContext ctx, FullHttpRequest request) {
            String[] str = StringUtils.split(request.uri(), ':');
            if (str.length != 2 || !validateTargetHost(str[0]) || !IntegerValidator.getInstance().isValid(str[1])) {
                throw new IllegalArgumentException("Illegal domain:" + request.uri());
            }

            ProxyRequest pr = new ProxyRequest(str[0], Integer.parseInt(str[1]), ctx.channel(), Protocol.TCP);
            assert parent != null;
            parent.publish(pr);

            ChannelPipeline pipeline = ctx.pipeline();

            pipeline.remove(this);
            ctx.write(new DefaultFullHttpResponse(request.protocolVersion(), CONNECTION_ESTABLISHED));
            pipeline.remove(HttpObjectAggregator.class);
            pipeline.remove(HttpServerCodec.class);
            pipeline.addLast(new TunnelProxyHandler(pr));
            ctx.flush();
        }


        /*private void processOtherMethod(ChannelHandlerContext ctx, FullHttpRequest request) throws MalformedURLException {
            URL url = new URL(request.uri());
            ProxyRequest pr = new ProxyRequest(url.getHost(), url.getPort(), ctx.channel(), Protocol.TCP);
        }*/


        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("Exception at HttpProxyRequestHandler");
            if (cause instanceof URISyntaxException) {
                ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST), ctx.voidPromise());
                ctx.close();
            } else {
                ctx.fireExceptionCaught(cause);
            }
        }
    }


    private static final class TunnelProxyHandler extends ChannelInboundHandlerAdapter {

        private final ProxyRequest proxyRequest;

        TunnelProxyHandler(ProxyRequest proxyRequest) {
            this.proxyRequest = Objects.requireNonNull(proxyRequest);
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


    /*private static final class PlaintextProxyHandler extends ChannelInboundHandlerAdapter {

        private final ProxyRequest proxyRequest;

        public PlaintextProxyHandler(ProxyRequest proxyRequest) {
            this.proxyRequest = Objects.requireNonNull(proxyRequest);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof FullHttpRequest) {
                try {
                    channelRead0(ctx, (FullHttpRequest) msg);
                } finally {
                    ReferenceCountUtil.release(msg);
                }
            }

            ctx.fireChannelRead(msg);
        }

        private void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            HttpHeaders headers = request.headers();
            headers.get(HttpHeaderNames.CONNECTION);

            DefaultFullHttpRequest message = new DefaultFullHttpRequest(request.protocolVersion(),
                    request.method(), request.uri());

        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            proxyRequest.close();
            ctx.fireChannelInactive();
        }
    }*/
}
