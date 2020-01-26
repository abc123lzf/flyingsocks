package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

/**
 * 服务器代理响应报文
 * 0
 * +-------------+-------+---------------+
 * |  Serial ID  |status |Message Length |
 * |  (2 Bytes)  |(1Byte)|   (4 Bytes)   |
 * +-------------+-------+---+-----------+
 * |               Message               |
 * |               Content               |
 * +-------------------------------------+
 */
public class ProxyResponseMessage extends ProxyMessage implements Message {

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
            this.head = (byte)head;
        }

        private static State getStateByHead(byte head) {
            for(State state : State.values())
                if(state.head == head)
                    return state;
            return null;
        }
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setMessage(ByteBuf buf) {
        super.message = buf;
    }

    @Override
    public ByteBuf serialize() throws SerializationException {
        if(state == null)
            throw new SerializationException("ProxyResponseMessage is not complete, message detail: \n" + toString());

        byte h = state.head;

        if(state == State.SUCCESS) {
            if(message == null) {
                throw new SerializationException("When ProxyResponseMessage's state is SUCCESS, message must not be null");
            }

            ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer(4 + 1 + 4 + message.readableBytes());
            buf.writeInt(serialId);
            buf.writeByte(h);
            buf.writeInt(message.readableBytes());
            buf.writeBytes(getMessage());

            return buf;
        } else {
            ByteBuf buf;
            if(message == null) {
                buf = PooledByteBufAllocator.DEFAULT.buffer(4 + 1 + 4);
            } else {
                buf = PooledByteBufAllocator.DEFAULT.buffer(4 + 1 + 4 + message.readableBytes());
            }

            buf.writeInt(serialId);

            buf.writeByte(h);
            if(message != null) {
                buf.writeInt(message.readableBytes());
                buf.writeBytes(getMessage());
            } else {
                buf.writeInt(0);
            }

            return buf;
        }
    }

    @Override
    public void deserialize(ByteBuf buf) throws SerializationException {
        try {
            int sid = buf.readInt();

            byte h = buf.readByte();
            State state = State.getStateByHead(h);

            ByteBuf msg;
            if(state == State.SUCCESS) {
                msg = PooledByteBufAllocator.DEFAULT.buffer(buf.readInt());
                buf.readBytes(msg);
            } else if(state == State.FAILURE) {
                int len = buf.readInt();
                if(len > 0) {
                    msg = PooledByteBufAllocator.DEFAULT.buffer(len);
                    buf.readBytes(msg);
                } else
                    msg = null;
            } else {
                throw new SerializationException("Unknown ProxyResponseMessage type " + h);
            }

            this.serialId = sid;
            this.message = msg;
            this.state = state;
        } catch (IndexOutOfBoundsException e) {
            throw new SerializationException("Illegal ProxyResponseMessage", e);
        }
    }

    @Override
    public String toString() {
        return "ProxyResponseMessage{" +
                "state=" + state +
                '}';
    }

    @Override
    protected void finalize() {
        ReferenceCountUtil.release(message);
    }
}
