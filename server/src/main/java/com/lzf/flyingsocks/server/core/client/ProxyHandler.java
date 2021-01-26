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

import com.lzf.flyingsocks.protocol.ProxyRequestMessage;
import com.lzf.flyingsocks.protocol.SerializationException;
import com.lzf.flyingsocks.server.core.ClientSession;
import com.lzf.flyingsocks.server.core.ProxyTask;
import com.lzf.flyingsocks.server.core.ProxyTaskManager;
import com.lzf.flyingsocks.util.HeartbeatMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/15 8:02
 */
class ProxyHandler extends ChannelInboundHandlerAdapter {

    public static final String HANDLER_NAME = "ProxyHandler";

    private static final String PROXY_REQUEST_FRAME_DECODER_NAME = "ProxyRequestFrameDecoder";

    private static final int REQUEST_MAX_FRAME = 1024 * 1024 * 20;

    private static final Logger log = LoggerFactory.getLogger(ProxyHandler.class);

    private ClientSession clientSession;

    private ProxyTaskManager proxyTaskManager;

    /**
     * IdleStateHandler -> [SslHandler] -> ClientSessionHandler -> FSMessageOutboundEncoder -> HeartbeatMessageHandler -> ProxyRequestFrameDecoder -> ProxyHandler
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        ClientSession session = ConnectionContext.clientSession(ctx.channel());
        ProxyTaskManager manager = ConnectionContext.proxyTaskManager(ctx.channel());
        if (session == null || manager == null) {
            throw new IllegalStateException();
        }

        this.clientSession = session;
        this.proxyTaskManager = manager;

        ChannelPipeline cp = ctx.pipeline();
        cp.addFirst(new IdleStateHandler(20, 0, 0));
        cp.addBefore(HANDLER_NAME, PROXY_REQUEST_FRAME_DECODER_NAME, new LengthFieldBasedFrameDecoder(REQUEST_MAX_FRAME,
                ProxyRequestMessage.LENGTH_OFFSET, ProxyRequestMessage.LENGTH_SIZE, ProxyRequestMessage.LENGTH_ADJUSTMENT, 0));
        cp.addBefore(PROXY_REQUEST_FRAME_DECODER_NAME, HeartbeatMessageHandler.NAME, HeartbeatMessageHandler.INSTANCE);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            try {
                channelRead0(ctx, buf);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }


    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws SerializationException {
        ProxyRequestMessage msg = new ProxyRequestMessage(buf);
        if (log.isDebugEnabled()) {
            log.debug("ProxyRequestMessage [{}:{}]", msg.getHost(), msg.getPort());
        }

        ProxyTask task = new ProxyTask(msg, clientSession);
        proxyTaskManager.publish(task);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof SerializationException) {
            log.warn("Serialize request occur a exception", cause);
        } else {
            log.error("An error occur in ProxyHandler", cause);
        }

        ctx.close();
    }
}
