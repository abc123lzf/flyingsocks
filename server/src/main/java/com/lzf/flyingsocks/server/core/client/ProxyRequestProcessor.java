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
package com.lzf.flyingsocks.server.core.client;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.encrypt.EncryptProvider;
import com.lzf.flyingsocks.misc.FSMessageOutboundEncoder;
import com.lzf.flyingsocks.server.core.ClientSession;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.resolver.ResolvedAddressTypes;
import io.netty.resolver.dns.DefaultAuthoritativeDnsServerCache;
import io.netty.resolver.dns.DefaultDnsCache;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.IntegerValidator;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/15 7:16
 */
class ProxyRequestProcessor extends AbstractComponent<ClientProcessor> {

    /**
     * 最大客户端数量
     */
    private final int maxClient;

    /**
     * 端口
     */
    private final int port;

    /**
     * 连接加密支持
     */
    private final EncryptProvider encryptProvider;

    /**
     * 客户Session管理器
     */
    private final ClientSessionHandler clientSessionHandler;


    /**
     * 代理服务Channel
     */
    private volatile ServerSocketChannel serverSocketChannel;


    /**
     * DNS解析工具，用于处理DNS请求
     */
    private DnsNameResolver dnsNameResolver;


    ProxyRequestProcessor(ClientProcessor processor, EncryptProvider encryptProvider) {
        super("ProxyRequestProcessor [" + processor.getName() + "]", Objects.requireNonNull(processor));
        this.port = parent.getParentComponent().getPort();
        this.maxClient = parent.getParentComponent().getMaxClient();
        this.encryptProvider = encryptProvider;
        this.clientSessionHandler = new ClientSessionHandler();
    }


    @Override
    protected void initInternal() {
        EventLoopGroup bossGroup = parent.getParentComponent().getBossWorker();
        // 初始化域名解析服务
        DnsNameResolverBuilder nameResolverBuilder = new DnsNameResolverBuilder(bossGroup.next());
        nameResolverBuilder.channelType(NioDatagramChannel.class)
                .resolveCache(new DefaultDnsCache())
                .resolvedAddressTypes(ResolvedAddressTypes.IPV4_PREFERRED)
                .authoritativeDnsServerCache(new DefaultAuthoritativeDnsServerCache())
                .recursionDesired(true);
        this.dnsNameResolver = nameResolverBuilder.build();

        super.initInternal();
    }

    @Override
    protected void startInternal() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup bossGroup = parent.getParentComponent().getBossWorker();
        EventLoopGroup childGroup = parent.getParentComponent().getChildWorker();

        // 初始化代理服务
        Class<? extends ServerSocketChannel> channelClass = parent.getParentComponent().getServerSocketChannelClass();
        ConfigManager<?> configManager = getConfigManager();
        String lowMarkStr = configManager.getSystemProperties("flyingsocks.client.watermark.low");
        String highMarkStr = configManager.getSystemProperties("flyingsocks.client.watermark.high");

        if (StringUtils.isBlank(lowMarkStr) || !IntegerValidator.getInstance().isValid(lowMarkStr)) {
            lowMarkStr = "1572864";
        }

        if (StringUtils.isBlank(highMarkStr) || !IntegerValidator.getInstance().isValid(highMarkStr)) {
            highMarkStr = "2097152";
        }

        WriteBufferWaterMark waterMark = new WriteBufferWaterMark(Integer.parseInt(lowMarkStr), Integer.parseInt(highMarkStr));

        bootstrap.group(bossGroup, childGroup)
                .channel(channelClass)
                .option(ChannelOption.AUTO_CLOSE, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ConnectionContext.initial(ch, parent.getParentComponent(), parent::doAuth, dnsNameResolver);
                        ChannelPipeline cp = ch.pipeline();
                        if (encryptProvider != null) {
                            Map<String, Object> params = Collections.singletonMap("alloc", ch.alloc());
                            if (!encryptProvider.isInboundHandlerSameAsOutboundHandler()) {
                                cp.addLast(encryptProvider.encodeHandler(params));
                            }
                            cp.addLast(encryptProvider.decodeHandler(params));
                        }

                        cp.addLast(FSMessageOutboundEncoder.INSTANCE);
                        cp.addLast(clientSessionHandler);
                        cp.addLast(ProxyAuthenticationHandler.HANDLER_NAME, new ProxyAuthenticationHandler());
                    }
                });

        ChannelFuture future = bootstrap.bind(port).awaitUninterruptibly();
        if (!future.isSuccess()) {
            throw new ComponentException(String.format("[%s] Bind failure", getName()), future.cause());
        }

        this.serverSocketChannel = (ServerSocketChannel) future.channel();
        super.startInternal();
    }



    @Override
    protected void stopInternal() {
        ServerSocketChannel serverChannel = this.serverSocketChannel;
        if (serverChannel != null) {
            serverChannel.close();
        }

        dnsNameResolver.close();
        dnsNameResolver = null;
        super.stopInternal();
    }

    /**
     * 用于管理客户端连接(ClientSession)
     */
    @ChannelHandler.Sharable
    class ClientSessionHandler extends ChannelInboundHandlerAdapter {

        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            ConnectionContext.putClientSession(ctx.channel(), new ClientSession(ctx.channel()));
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {

            ConnectionContext.clientSession(ctx.channel()).updateLastActiveTime();
            ctx.fireChannelRead(msg);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            if (counter.incrementAndGet() > maxClient) {
                log.info("Node \"{}\" Client number out of maxClient limit, value:{}", getName(), maxClient);
                ctx.close();
            }

            ctx.fireChannelActive();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof SSLException || cause.getCause() instanceof SSLException) {
                log.info("Client connection is not SSL Connection");
            } else if (cause instanceof IOException) {
                log.info("Remote host close the connection");
            } else if (log.isWarnEnabled()) {
                log.warn("An exception occur", cause);
            }
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            counter.decrementAndGet();
            ctx.fireChannelInactive();
        }
    }
}
