package com.lzf.flyingsocks;

import io.netty.channel.socket.SocketChannel;

/**
 * 会话模板类
 * @see com.lzf.flyingsocks.Session
 */
public abstract class AbstractSession implements Session {

    /**
     * 用于通讯的SocketChannel
     */
    protected final SocketChannel socketChannel;

    /**
     * 连接建立时间
     */
    protected final long connectionTime;

    /**
     * 上次活跃时间
     */
    protected volatile long lastActiveTime;


    protected AbstractSession(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        this.connectionTime = System.currentTimeMillis();
        this.lastActiveTime = lastActiveTime();
    }

    @Override
    public final long connectionTime() {
        return connectionTime;
    }

    @Override
    public final long lastActiveTime() {
        return lastActiveTime;
    }

    @Override
    public SocketChannel socketChannel() {
        return socketChannel;
    }
}
