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
package com.lzf.flyingsocks.client.proxy.socks;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/12 5:38
 */
class UdpProxyMessage {

    private final String host;

    private final int port;

    private ByteBuf data;

    public UdpProxyMessage(String host, int port, ByteBuf data) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port number:" + port);
        }

        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.data = Objects.requireNonNull(data);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public ByteBuf getData() {
        return data;
    }

    public boolean isSameTarget(UdpProxyMessage message) {
        return host.equals(message.host) && port == message.port;
    }


    public void release() {
        ByteBuf buf = this.data;
        if (buf != null) {
            data.release();
            this.data = null;
        }
    }

}
