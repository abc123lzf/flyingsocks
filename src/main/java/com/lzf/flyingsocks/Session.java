package com.lzf.flyingsocks;

import io.netty.channel.socket.SocketChannel;

import java.net.InetSocketAddress;

public interface Session {

    /**
     * @return 连接建立时间
     */
    long connectionTime();

    /**
     * @return 上次活跃时间
     */
    long lastActiveTime();

    /**
     * @return 该连接的Socket通道
     */
    SocketChannel socketChannel();

    /**
     * @return 该连接Channel通道是否活跃
     */
    default boolean isActive() {
        return socketChannel().isActive();
    }

    /**
     * 获取远程连接地址
     * @return InetSocketAddress对象
     */
    default InetSocketAddress remoteAddress() {
        return socketChannel().remoteAddress();
    }

    /**
     * 获取本地连接地址
     * @return 本地连接地址
     */
    default InetSocketAddress localAddress() {
        return socketChannel().localAddress();
    }
}
