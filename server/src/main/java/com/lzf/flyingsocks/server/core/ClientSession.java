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
package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.AbstractSession;
import com.lzf.flyingsocks.Session;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;

/**
 * 客户端会话对象
 */
public class ClientSession extends AbstractSession implements Session {

    /**
     * 该客户端是否通过认证
     */
    private boolean auth = false;

    public ClientSession(Channel channel) {
        super((SocketChannel) channel);
    }

    public boolean isWriteable() {
        return socketChannel.isWritable();
    }

    public void writeMessage(Object msg) {
        checkChannelState();
        socketChannel.write(msg, socketChannel.voidPromise());
    }

    public void writeAndFlushMessage(Object msg) {
        checkChannelState();
        socketChannel.writeAndFlush(msg, socketChannel.voidPromise());
    }

    public void flushMessage() {
        checkChannelState();
        socketChannel.flush();
    }

    public boolean isAuth() {
        return auth;
    }

    public void passAuth() {
        auth = true;
    }

    public void updateLastActiveTime() {
        lastActiveTime = System.currentTimeMillis();
    }

    private void checkChannelState() throws IllegalStateException {
        if (!isActive())
            throw new IllegalStateException("Channel has been closed");
    }

    @Override
    public String toString() {
        return "ClientSession [ address:" + remoteAddress() +
                " ,connection_time:" + connectionTime +
                " ,last_active_time:" + lastActiveTime +
                " ,auth:" + auth + " ]";
    }
}
