package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

/**
 * 代理消息抽象类
 */
abstract class ProxyMessage implements Message {

    static final Charset CHANNEL_ENCODING = Charset.forName("ASCII");

    /**
     * 客户端发起代理请求的Channel Id，编码格式为ASCII
     */
    String channelId;

    /**
     * 请求/响应内容
     */
    ByteBuf message;


    ProxyMessage(String channelId) {
        this.channelId = channelId;
    }

    ProxyMessage(ByteBuf serialBuf) throws SerializationException {
        deserialize(serialBuf);
    }

    /**
     * @return 客户端发起代理请求的Channel ID，编码为ASCII
     */
    public final String getChannelId() {
        return channelId;
    }

    /**
     * @return 代理消息请求/响应正文
     */
    public final ByteBuf getMessage() {
        if(message != null) {
            ByteBuf buf = message;
            message = null;
            return buf;
        }

        throw new UnsupportedOperationException("ProxyMessage content is only can get one time");
    }

}
