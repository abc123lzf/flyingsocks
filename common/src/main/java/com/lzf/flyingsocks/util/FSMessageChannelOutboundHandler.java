package com.lzf.flyingsocks.util;

import com.lzf.flyingsocks.protocol.Message;
import com.lzf.flyingsocks.protocol.SerializationException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class FSMessageChannelOutboundHandler extends ChannelOutboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger("MessageSerializeHandler");

    public static final FSMessageChannelOutboundHandler INSTANCE = new FSMessageChannelOutboundHandler();

    private FSMessageChannelOutboundHandler() {}

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if(msg instanceof Message) {
            Message message = (Message) msg;
            try {
                ByteBuf buf = message.serialize();
                ctx.write(buf, ctx.voidPromise());
            } catch (SerializationException e) {
                log.error("Serialize exception occur, with type: {}", message.getClass().getName(), e);
            }
        } else {
            ctx.write(msg, ctx.voidPromise());
        }
    }
}
