package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

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


    public final String getChannelId() {
        return channelId;
    }


    public final ByteBuf getMessage() {
        if(message != null) {
            ByteBuf buf = message;
            message = null;
            return buf;
        }

        throw new UnsupportedOperationException("ProxyMessage content is only can get one time");
    }


}
