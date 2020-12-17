package com.lzf.flyingsocks.client.proxy.util;

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

    private volatile boolean cancel = false;

    private volatile Queue<ByteBuf> transferCache = new ArrayDeque<>();

    private volatile MessageReceiver messageReceiver;

    private final Object lock = new Object();

    public void transfer(ByteBuf buf) throws MessageDelivererCancelledException {
        Objects.requireNonNull(buf);

        if (cancel) {
            throw new MessageDelivererCancelledException();
        }

        if (messageReceiver != null) {
            messageReceiver.receive(buf);
            return;
        }

        synchronized (lock) {
            if (cancel) {
                throw new MessageDelivererCancelledException();
            }

            if (messageReceiver == null) {
                transferCache.offer(buf);
                return;
            }

            messageReceiver.receive(buf);
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
        synchronized (lock) {
            if (cancel) {
                throw new MessageDelivererCancelledException();
            }

            while (!transferCache.isEmpty()) {
                ByteBuf buf = transferCache.poll();
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

        synchronized (lock) {
            cancel = true;
            if (messageReceiver != null) {
                messageReceiver.close();
            }

            if (transferCache != null) {
                transferCache.forEach(ByteBuf::release);
                transferCache = null;
            }
        }
    }

    @Override
    protected void finalize() {
        if (transferCache != null && !transferCache.isEmpty()) {
            transferCache.forEach(ByteBuf::release);
        }
    }
}
