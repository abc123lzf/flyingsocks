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
import com.lzf.flyingsocks.protocol.CertRequestMessage;
import com.lzf.flyingsocks.util.FSMessageChannelOutboundHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Objects;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/15 4:47
 */
class CertRequestProcessor extends AbstractComponent<ClientProcessor> {

    /**
     * 服务端口
     */
    private final int port;

    /**
     * 客户端请求处理器
     */
    private final CertRequestHandler certRequestHandler;

    /**
     * 服务绑定端口后对应的ServerSocketChannel
     */
    private volatile ServerSocketChannel serverSocketChannel;


    CertRequestProcessor(ClientProcessor processor, byte[] certFile) {
        super("CertRequestProcessor", Objects.requireNonNull(processor));
        this.port = parent.getParentComponent().getCertPort();
        this.certRequestHandler = new CertRequestHandler(certFile, parent::doAuth);
    }


    @Override
    protected void initInternal() {
        super.initInternal();
    }

    @Override
    protected void startInternal() {
        final CertRequestHandler certRequestHandler = this.certRequestHandler;
        ServerBootstrap certBoot = new ServerBootstrap();

        EventLoopGroup group = parent.getParentComponent().getChildWorker();
        Class<? extends ServerSocketChannel> channelClass;
        if (group instanceof EpollEventLoopGroup) {
            channelClass = EpollServerSocketChannel.class;
        } else if (group instanceof KQueueEventLoopGroup) {
            channelClass = KQueueServerSocketChannel.class;
        } else {
            channelClass = NioServerSocketChannel.class;
        }

        certBoot.group(group).channel(channelClass)
                .option(ChannelOption.AUTO_CLOSE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline cp = ch.pipeline();
                        int l = CertRequestMessage.END_MARK.length;
                        ByteBuf buf = Unpooled.buffer(l);
                        buf.writeBytes(CertRequestMessage.END_MARK);

                        cp.addLast(new IdleStateHandler(15, 0, 0));
                        cp.addLast(IdleStateEventHandler.INSTANCE);
                        cp.addLast(FSMessageChannelOutboundHandler.INSTANCE);
                        cp.addLast(new DelimiterBasedFrameDecoder(512 + l, buf));
                        cp.addLast(certRequestHandler);
                    }
                });

        ChannelFuture future = certBoot.bind(port).awaitUninterruptibly();
        if (!future.isSuccess()) {
            log.error("Bind port {} failure", port);
            throw new ComponentException(future.cause());
        }

        this.serverSocketChannel = (ServerSocketChannel) future.channel();
        super.startInternal();
    }

    @Override
    protected void stopInternal() {
        ServerSocketChannel channel = this.serverSocketChannel;
        if (channel != null) {
            channel.close();
        }

        super.stopInternal();
    }


    @ChannelHandler.Sharable
    private static class IdleStateEventHandler extends ChannelInboundHandlerAdapter {
        static final IdleStateEventHandler INSTANCE = new IdleStateEventHandler();
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                ctx.close();
            } else {
                ctx.fireUserEventTriggered(evt);
            }
        }
    }
}
