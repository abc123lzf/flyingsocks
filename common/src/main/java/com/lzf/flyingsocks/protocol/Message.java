package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * 统一报文接口，用于客户端与服务端之间的通信
 */
public interface Message {

    /**
     * 将报文对象序列化为ByteBuf
     * @return 序列化后的ByteBuf
     * @throws SerializationException 序列化异常
     */
    ByteBuf serialize() throws SerializationException;


    /**
     * 反序列化
     * @param buf Netty的ByteBuf对象
     * @throws SerializationException 反序列化异常
     */
    void deserialize(ByteBuf buf) throws SerializationException;


    /**
     * @return 默认的ByteBuf分配器
     */
    default ByteBufAllocator getAllocator() {
        return PooledByteBufAllocator.DEFAULT;
    }
}
