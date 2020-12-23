package com.lzf.flyingsocks.client.proxy.util;

import io.netty.buffer.ByteBuf;

/**
 * @author lzf abc123lzf@126.com
 * @since 2020/12/16 20:56
 */
public interface MessageReceiver {

    /**
     * 接收来自客户端的消息
     *
     * @param message 客户端消息体
     */
    void receive(ByteBuf message);

    /**
     * {@link MessageDeliverer} 关闭后的行为
     */
    default void close() {
    }
}
