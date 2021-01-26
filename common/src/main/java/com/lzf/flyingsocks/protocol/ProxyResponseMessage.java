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
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 服务器代理响应报文
 * 0
 * +----+-------+-------+---------------+
 * |HEAD|  SID  |status |Message Length |
 * | 1B |   4B  |(1Byte)|   (4 Bytes)   |
 * +----+-------+-------+---------------+
 * |               Message              |
 * |               Content              |
 * +------------------------------------+
 */
public class ProxyResponseMessage extends ProxyMessage {

    public static final int LENGTH_OFFSET = 1 + 4 + 1;  // HEAD + SID + status

    public static final int LENGTH_SIZE = 4;  // sizeof(int)

    public static final int LENGTH_ADJUSTMENT = 0;

    /**
     * HEAD，用于区分不同类型的消息
     */
    public static final byte HEAD = 0x00;

    /**
     * 连接状态
     */
    private State state;

    public ProxyResponseMessage(int serialId) {
        super(serialId);
    }

    public ProxyResponseMessage(ByteBuf serialBuf) throws SerializationException {
        super(serialBuf);
    }

    public enum State {
        SUCCESS(0x00), FAILURE(0x01);

        private final byte head;

        State(int head) {
            this.head = (byte) head;
        }

        private static State getStateByHead(byte head) {
            for (State state : State.values()) {
                if (state.head == head) {
                    return state;
                }
            }
            return null;
        }
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public ByteBuf serialize(ByteBufAllocator allocator) throws SerializationException {
        State state = this.state;
        assertTrue(state != null, "ProxyResponseMessage is not complete, message detail: \n" + toString());

        byte h = state.head;
        ByteBuf message = getMessage();
        int sid = this.serialId;

        if (state == State.SUCCESS) {
            assertTrue(message != null, "When ProxyResponseMessage's state is SUCCESS, message must not be null");
            CompositeByteBuf result = allocator.compositeBuffer(2);
            ByteBuf header = allocator.buffer(1 + 4 + 1 + 4);
            header.writeByte(HEAD);
            header.writeInt(sid);
            header.writeByte(h);
            header.writeInt(message.readableBytes());

            result.addComponent(true, header);
            result.addComponent(true, message);
            return result;
        } else {
            ByteBuf header = allocator.buffer(1 + 4 + 1 + 4);
            header.writeByte(HEAD);
            header.writeInt(sid);
            header.writeByte(h);

            if (message == null) {
                header.writeInt(0);
                return header;
            } else {
                header.writeInt(message.readableBytes());
            }

            CompositeByteBuf result = allocator.compositeBuffer(2);
            result.addComponent(true, header);
            result.addComponent(true, message);
            return result;
        }
    }

    @Override
    protected void deserialize(ByteBuf buf) throws SerializationException {
        try {
            checkoutHeadField(buf);
            int sid = buf.readInt();
            byte h = buf.readByte();
            State state = State.getStateByHead(h);
            int len = buf.readInt();

            ByteBuf msg;
            if (state == State.SUCCESS) {
                msg = buf.readRetainedSlice(len);
            } else if (state == State.FAILURE) {
                if (len > 0) {
                    msg = buf.readRetainedSlice(len);
                } else {
                    msg = Unpooled.wrappedBuffer(new byte[0]);
                }
            } else {
                throw new SerializationException("Unknown ProxyResponseMessage type " + h);
            }

            this.serialId = sid;
            setMessage(msg);
            this.state = state;
        } catch (IndexOutOfBoundsException e) {
            throw new SerializationException("Illegal ProxyResponseMessage", e);
        }
    }


    private void checkoutHeadField(ByteBuf buf) throws SerializationException {
        byte head = buf.readByte();
        if (head != HEAD) {
            throw new SerializationException(ProxyResponseMessage.class, "Illegal head value:" + head);
        }
    }

    @Override
    public String toString() {
        return "ProxyResponseMessage{" +
                "state=" + state +
                '}';
    }

}
