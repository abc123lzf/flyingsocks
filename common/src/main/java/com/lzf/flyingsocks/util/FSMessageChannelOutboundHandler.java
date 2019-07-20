package com.lzf.flyingsocks.util;

import com.lzf.flyingsocks.protocol.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

@ChannelHandler.Sharable
public class FSMessageChannelOutboundHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof Message) {
            Message message = (Message) msg;
            ByteBuf buf = message.serialize();
            ctx.write(buf, ctx.voidPromise());
        } else {
            ctx.write(msg, ctx.voidPromise());
        }
    }
}
