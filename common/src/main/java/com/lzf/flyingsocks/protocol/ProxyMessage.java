package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;

/**
 * 代理消息抽象类
 */
abstract class ProxyMessage implements Message {

    /**
     * 客户端发起代理请求的序列ID
     */
    int serialId;

    /**
     * 请求/响应内容
     */
    ByteBuf message;


    ProxyMessage(int serialId) {
        this.serialId = serialId;
    }

    ProxyMessage(ByteBuf serialBuf) throws SerializationException {
        deserialize(serialBuf);
    }

    /**
     * @return 客户端发起代理请求的序列ID
     */
    public final int serialId() {
        return serialId;
    }

    /**
     * @return 代理消息请求/响应正文
     */
    public final ByteBuf getMessage() {
        if (message != null) {
            ByteBuf buf = message;
            message = null;
            return buf;
        }

        throw new UnsupportedOperationException("ProxyMessage content is only can get one time");
    }

    /**
     * 当condition为false时抛出SerializationException
     */
    static void assertTrue(boolean condition, String errorMessage) throws SerializationException {
        if (!condition) {
            throw new SerializationException(errorMessage);
        }
    }

}
