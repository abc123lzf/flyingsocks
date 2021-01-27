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
package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;

/**
 * 代理消息抽象类
 */
abstract class ProxyMessage extends ServiceStageMessage {

    private static final byte SERVICE_ID = 0;

    /**
     * 客户端发起代理请求的序列ID
     */
    int serialId;

    /**
     * 请求/响应内容
     */
    private ByteBuf message;


    ProxyMessage(int serialId) {
        super(SERVICE_ID);
        this.serialId = serialId;
    }


    ProxyMessage(ByteBuf serialBuf) throws SerializationException {
        super(serialBuf);
    }

    /**
     * @return 客户端发起代理请求的序列ID
     */
    public final int serialId() {
        return serialId;
    }

    /**
     * @return 代理消息请求/响应正文
     */
    public synchronized final ByteBuf getMessage() {
        if (message != null) {
            ByteBuf buf = message;
            message = null;
            return buf;
        }

        throw new IllegalStateException("ProxyMessage content is only can get one time");
    }

    public synchronized final void setMessage(ByteBuf buf) {
        if (message != null) {
            throw new IllegalStateException("message has set");
        }

        this.message = buf;
    }

    /**
     * 当condition为false时抛出SerializationException
     */
    static void assertTrue(boolean condition, String errorMessage) throws SerializationException {
        if (!condition) {
            throw new SerializationException(errorMessage);
        }
    }

}
