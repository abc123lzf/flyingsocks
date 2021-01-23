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

import com.lzf.flyingsocks.AbstractSession;
import com.lzf.flyingsocks.protocol.DelimiterMessage;
import io.netty.channel.socket.SocketChannel;

import java.util.Arrays;

public class ProxyServerSession extends AbstractSession {

    /**
     * 协议分隔符
     */
    private byte[] delimiter;

    ProxyServerSession(SocketChannel serverChannel) {
        super(serverChannel);
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    void setDelimiter(byte[] delimiter) {
        if (delimiter == null || delimiter.length != DelimiterMessage.DEFAULT_SIZE)
            throw new IllegalArgumentException("Delimiter length must be " + DelimiterMessage.DEFAULT_SIZE + " bytes");
        this.delimiter = Arrays.copyOf(delimiter, DelimiterMessage.DEFAULT_SIZE);
    }

    byte[] getDelimiter() {
        if (delimiter == null) {
            return null;
        }

        return Arrays.copyOf(delimiter, delimiter.length);
    }
}
