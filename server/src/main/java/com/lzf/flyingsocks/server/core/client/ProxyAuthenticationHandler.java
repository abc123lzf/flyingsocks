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

import com.lzf.flyingsocks.protocol.AuthRequestMessage;
import com.lzf.flyingsocks.protocol.AuthResponseMessage;
import com.lzf.flyingsocks.protocol.SerializationException;
import com.lzf.flyingsocks.server.core.ClientSession;
import com.lzf.flyingsocks.misc.MessageHeaderCheckHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/15 7:47
 */
public class ProxyAuthenticationHandler extends ChannelInboundHandlerAdapter {

    public static final String HANDLER_NAME = "ProxyAuthenticationHandler";

    private static final String AUTH_REQUEST_FRAME_DECODER_NAME = "AuthRequestMessageFrameDecoder";

    private static final String IDLE_HANDLER_NAME = "AuthRequestIdleHandler";

    private static final String AUTH_REQUEST_HEADER_CHECKER_NAME = "AuthRequestHeaderChecker";

    private static final MessageHeaderCheckHandler AUTH_REQUEST_HEADER_CHECKER = new MessageHeaderCheckHandler(AuthRequestMessage.getMessageHeader());

    private static final Logger log = LoggerFactory.getLogger(ProxyAuthenticationHandler.class);

    private ClientSession clientSession;

    ProxyAuthenticationHandler() {
        super();
    }

    /**
     * IdleStateHandler -> [SslHandler] -> ClientSessionHandler -> FSMessageOutboundEncoder -> AuthRequestMessageFrameDecoder -> ProxyAuthenticationHandler
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        this.clientSession = ConnectionContext.clientSession(channel);

        ChannelPipeline cp = ctx.pipeline();
        cp.addFirst(IDLE_HANDLER_NAME, new IdleStateHandler(10, 0, 0));
        cp.addBefore(HANDLER_NAME, AUTH_REQUEST_FRAME_DECODER_NAME,
                new LengthFieldBasedFrameDecoder(Short.MAX_VALUE, AuthRequestMessage.LENGTH_OFFSET,
                        AuthRequestMessage.LENGTH_SIZE, AuthRequestMessage.LENGTH_ADJUSTMENT, 0));
        cp.addBefore(AUTH_REQUEST_FRAME_DECODER_NAME, AUTH_REQUEST_HEADER_CHECKER_NAME, AUTH_REQUEST_HEADER_CHECKER);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            try {
                channelRead0(ctx, (ByteBuf) msg);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            ctx.close();
            return;
        }

        ctx.fireUserEventTriggered(evt);
    }

    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws SerializationException {
        ClientSession session = this.clientSession;
        Predicate<AuthRequestMessage> authPredicate = ConnectionContext.authPredicate(ctx.channel());

        AuthRequestMessage msg = new AuthRequestMessage(buf);
        boolean auth = authPredicate.test(msg);
        if (!auth) {
            if (log.isTraceEnabled()) {
                log.trace("Auth failure, from client {}", ((SocketChannel) ctx.channel()).remoteAddress().getHostName());
            }

            AuthResponseMessage response = new AuthResponseMessage(false);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace("Auth success, from client {}", ((SocketChannel) ctx.channel()).remoteAddress().getHostName());
        }

        session.passAuth();

        ctx.write(new AuthResponseMessage(true), ctx.voidPromise());

        ChannelPipeline cp = ctx.pipeline();
        cp.remove(this);
        cp.addLast(ProxyHandler.HANDLER_NAME, new ProxyHandler());
        ctx.flush();
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        ChannelPipeline cp = ctx.pipeline();
        cp.remove(AUTH_REQUEST_HEADER_CHECKER_NAME);
        cp.remove(IDLE_HANDLER_NAME);
        cp.remove(AUTH_REQUEST_FRAME_DECODER_NAME);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof SerializationException) {
            log.info("An exception occur when deserialize AuthRequestMessage", cause);
            ctx.close();
            return;
        }

        log.warn("An exception occur in ProxyAuthenticationHandler", cause);
    }
}
