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
package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/20 21:04
 */
public class PingMessage implements Message {

    public static final byte HEAD = 0x01;

    private static final byte[] CONTENT = "PING".getBytes(StandardCharsets.US_ASCII);

    private static final ByteBuf BODY;

    static {
        ByteBuf body = Unpooled.directBuffer(1 + CONTENT.length);
        body.writeByte(HEAD);
        body.writeBytes(CONTENT);
        BODY = body.asReadOnly();
    }

    @Override
    public ByteBuf serialize(ByteBufAllocator allocator) throws SerializationException {
        return BODY.retainedSlice();
    }

    @Override
    public void deserialize(ByteBuf buf) throws SerializationException {
        try {
            byte header = buf.readByte();
            if (header != HEAD) {
                throw new SerializationException(PingMessage.class, "Illegal header: " + header);
            }

            byte[] content = new byte[CONTENT.length];
            buf.readBytes(content);
            if (!Arrays.equals(content, CONTENT)) {
                throw new SerializationException(PingMessage.class, "Illegal content: " + Arrays.toString(content));
            }
        } catch (IndexOutOfBoundsException e) {
            throw new SerializationException("Unable to read ping message", e);
        }
    }
}
