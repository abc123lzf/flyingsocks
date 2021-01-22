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
package com.lzf.flyingsocks;

import io.netty.channel.socket.SocketChannel;

/**
 * 会话模板类
 *
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
