package com.lzf.flyingsocks;

import io.netty.channel.socket.SocketChannel;

public abstract class AbstractSession implements Session {

    protected final SocketChannel socketChannel;

    protected final long connectionTime;

    protected volatile long lastActiveTime;

    protected AbstractSession(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        this.connectionTime = System.currentTimeMillis();
        this.lastActiveTime = lastActiveTime();
    }

    @Override
    public long connectionTime() {
        return connectionTime;
    }

    @Override
    public long lastActiveTime() {
        return lastActiveTime;
    }

    @Override
    public SocketChannel socketChannel() {
        return socketChannel;
    }
}
