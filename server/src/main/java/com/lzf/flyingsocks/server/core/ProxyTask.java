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

import com.lzf.flyingsocks.protocol.ProxyRequestMessage;

import java.util.Objects;

/**
 * 代理任务对象
 */
public class ProxyTask {

    private final ProxyRequestMessage proxyRequestMessage;

    private final ClientSession session;

    public ProxyTask(ProxyRequestMessage proxyRequestMessage, ClientSession clientSession) {
        this.proxyRequestMessage = Objects.requireNonNull(proxyRequestMessage, "ProxyRequestMessage must not be null");
        this.session = Objects.requireNonNull(clientSession, "ClientSession must not be null");
    }

    public ProxyRequestMessage getRequestMessage() {
        return proxyRequestMessage;
    }

    public ClientSession session() {
        return session;
    }

    @Override
    public int hashCode() {
        return session.hashCode() ^ proxyRequestMessage.getHost().hashCode() ^
                (proxyRequestMessage.getPort() << 16) ^ proxyRequestMessage.getProtocol().hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public String toString() {
        return "ProxyTask{" +
                "proxyRequestMessage=" + proxyRequestMessage +
                ", session=" + session +
                '}';
    }
}
