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

import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;

import java.util.Arrays;

/**
 * 用于实现UDP穿透中的分帧
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/11 6:42
 */
interface ReassemblyQueue {

    /**
     * @param frag FRAG字段
     * @param message UDP报文
     * @return 整合后的UDP代理数据，如果返回null，则消息不完整或者超时
     */
    UdpProxyMessage tryAppendAndGet(byte frag, UdpProxyMessage message);

    /**
     * 重置ReassemblyQueue
     */
    void reset();
}


class BaseReassemblyQueue implements ReassemblyQueue {

    private UdpProxyMessage[] queue;

    private long lastAppendTime;

    private int nextFragId;

    public BaseReassemblyQueue() {
        this.lastAppendTime = -1;
        this.nextFragId = 1;
    }

    public UdpProxyMessage tryAppendAndGet(byte frag, UdpProxyMessage message) {
        if (lastAppendTime == -1) {
            lastAppendTime = System.currentTimeMillis();
        }

        if (queue == null) {
            queue = new UdpProxyMessage[4];
        }

        int index = frag & 0x7F;

        long now = System.currentTimeMillis();
        // frag不符合预期时抛弃
        if (index != nextFragId || now - lastAppendTime > 5000) {
            message.release();
            reset();
            return null;
        }

        if (queue[0] != null && !queue[0].isSameTarget(message)) {
            message.release();
            reset();
            return null;
        }

        // 检测Queue大小是否足够
        if (index - 1 >= queue.length) {
            if (queue.length * 2 > 128) {
                message.release();
                reset();
                return null;
            }

            UdpProxyMessage[] oldQueue = this.queue;
            this.queue = Arrays.copyOf(oldQueue, oldQueue.length * 2);
        }

        lastAppendTime = now;

        queue[index - 1] = message.retain();
        nextFragId++;

        // 最高位为1
        if (frag < 0) {
            UdpProxyMessage result = combine();
            reset();
            return result;
        }

        return null;
    }


    private UdpProxyMessage combine() {
        UdpProxyMessage first = queue[0];
        String host = first.getHost();
        int port = first.getPort();

        CompositeByteBuf buf = Unpooled.compositeBuffer(nextFragId - 1);
        for (UdpProxyMessage msg : queue) {
            buf.addComponent(true, msg.getData());
        }

        return new UdpProxyMessage(host, port, buf);
    }


    public void reset() {
        if (queue != null) {
            for (UdpProxyMessage msg : queue) {
                if (msg != null) {
                    ReferenceCountUtil.release(msg);
                }
            }
        }

        queue = null;
        lastAppendTime = -1;
        nextFragId = 1;
    }

    @Override
    protected void finalize() {
        reset();
    }
}
