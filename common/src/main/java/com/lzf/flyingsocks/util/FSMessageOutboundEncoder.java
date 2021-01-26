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
public class FSMessageOutboundEncoder extends ChannelOutboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger("FSMessageOutboundEncoder");

    public static final String HANDLER_NAME = "FSMessageOutboundEncoder";

    public static final FSMessageOutboundEncoder INSTANCE = new FSMessageOutboundEncoder();

    private FSMessageOutboundEncoder() {
        super();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof Message) {
            Message message = (Message) msg;

            try {
                ByteBuf buf = message.serialize(ctx.alloc());
                ctx.write(buf, promise);
            } catch (SerializationException e) {
                log.error("Serialize exception occur, with type: {}", message.getClass().getName(), e);
            }
        } else {
            ctx.write(msg, promise);
        }
    }

    @Override
    public boolean isSharable() {
        return true;
    }
}
