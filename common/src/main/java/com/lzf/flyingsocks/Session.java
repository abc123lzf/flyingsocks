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

import java.net.InetSocketAddress;

/**
 * 用来保存网络连接中的会话
 */
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
     *
     * @return InetSocketAddress对象
     */
    default InetSocketAddress remoteAddress() {
        return socketChannel().remoteAddress();
    }

    /**
     * 获取本地连接地址
     *
     * @return 本地连接地址
     */
    default InetSocketAddress localAddress() {
        return socketChannel().localAddress();
    }
}
