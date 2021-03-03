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
package com.lzf.flyingsocks.client.proxy.misc;

import io.netty.buffer.ByteBuf;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

/**
 * 消息传递工具
 *
 * @author lzf abc123lzf@126.com
 * @since 2020/12/16 20:52
 */
public class MessageDeliverer {

    /**
     * MessageDeliverer是否被关闭
     */
    private volatile boolean cancel = false;

    /**
     * 消息接收者尚未设置时缓存 {@link ByteBuf}
     */
    private volatile Queue<ByteBuf> transferCache = new ArrayDeque<>();

    /**
     * 消息接收者
     */
    private volatile MessageReceiver messageReceiver;


    public void transfer(ByteBuf buf) throws MessageDelivererCancelledException {
        Objects.requireNonNull(buf);

        if (cancel) {
            throw new MessageDelivererCancelledException();
        }

        MessageReceiver receiver = this.messageReceiver;
        if (receiver != null) {
            receiver.receive(buf.retain());
            return;
        }

        synchronized (this) {
            if (cancel) {
                throw new MessageDelivererCancelledException();
            }

            receiver = this.messageReceiver;
            if (receiver == null) {
                transferCache.offer(buf.retain());
                return;
            }

            receiver.receive(buf.retain());
        }
    }


    public void setReceiver(MessageReceiver receiver) throws MessageDelivererCancelledException {
        if (messageReceiver != null) {
            throw new IllegalStateException("Receiver has set");
        }

        if (cancel) {
            throw new MessageDelivererCancelledException();
        }

        Objects.requireNonNull(receiver);
        synchronized (this) {
            if (cancel) {
                throw new MessageDelivererCancelledException();
            }

            Queue<ByteBuf> cache = this.transferCache;
            while (!cache.isEmpty()) {
                ByteBuf buf = cache.poll();
                receiver.receive(buf);
            }

            this.messageReceiver = receiver;
            this.transferCache = null;
        }
    }


    public void cancel() {
        if (cancel) {
            return;
        }

        synchronized (this) {
            cancel = true;
            MessageReceiver receiver = this.messageReceiver;
            if (receiver != null) {
                receiver.close();
            }

            Queue<ByteBuf> cache = this.transferCache;
            if (cache != null) {
                cache.forEach(ByteBuf::release);
                this.transferCache = null;
            }
        }
    }

    @Override
    protected void finalize() {
        Queue<ByteBuf> cache = this.transferCache;
        if (cache != null && !cache.isEmpty()) {
            cache.forEach(ByteBuf::release);
        }
    }
}
