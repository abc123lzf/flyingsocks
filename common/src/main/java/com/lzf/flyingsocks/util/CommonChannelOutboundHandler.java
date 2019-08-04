package com.lzf.flyingsocks.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;

import java.net.SocketAddress;

/**
 *
 */
public interface CommonChannelOutboundHandler extends ChannelOutboundHandler {

    @Override
    default void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }

    @Override
    default void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    default void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.disconnect(promise);
    }

    @Override
    default void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.close(promise);
    }

    @Override
    default void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.deregister(promise);
    }

    @Override
    default void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    default void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
    }

    @Override
    default void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    default void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        //NOOP
    }

    @Override
    default void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        //NOOP
    }

    @Override
    default void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireExceptionCaught(cause);
    }
}
