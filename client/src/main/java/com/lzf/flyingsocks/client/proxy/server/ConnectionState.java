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
package com.lzf.flyingsocks.client.proxy.server;

/**
 * 代理服务器连接状态
 */
public enum ConnectionState {

    NEW(true, false),
    SSL_INITIAL(true, false),
    SSL_CONNECTING(true, false),
    SSL_CONNECT_TIMEOUT(false, true),
    SSL_CONNECT(true, false),
    SSL_CONNECT_AUTH_FAILURE(false, false),
    SSL_CONNECT_ERROR(false, false),
    SSL_CONNECT_DONE(true, false),
    PROXY_INITIAL(true, false),
    PROXY_CONNECTING(true, false),
    PROXY_CONNECT_TIMEOUT(false, true),
    PROXY_CONNECT(true, false),
    PROXY_DISCONNECT(false, true),
    PROXY_CONNECT_AUTH_FAILURE(false, false),
    PROXY_CONNECT_ERROR(false, false),
    UNUSED(true, false);


    /**
     * 当前状态是否是正常的
     */
    private final boolean normal;

    /**
     * 是否可重试连接
     */
    private final boolean retrievable;


    public boolean isNormal() {
        return normal;
    }

    public boolean canRetry() {
        return retrievable;
    }

    ConnectionState(boolean normal, boolean retrievable) {
        this.retrievable = retrievable;
        this.normal = normal;
    }
}
