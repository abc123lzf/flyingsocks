package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.client.proxy.ProxyRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class SocksProxyRequest extends ProxyRequest {

    /**
     * 当无需进行代理直接与目标服务器(例如www.baidu.com)连接时的Channel对象
     */
    private Channel serverChannel;

    private final BlockingQueue<ByteBuf> messageQueue;

    SocksProxyRequest(String host, int port, Channel channel) {
        super(Objects.requireNonNull(host), port, Objects.requireNonNull(channel));
        messageQueue = new LinkedBlockingQueue<>();
    }

    @Override
    protected Channel getClientChannel() {
        return super.getClientChannel();
    }

    @Override
    protected void closeClientChannel() {
        super.closeClientChannel();
    }

    @Override
    protected void setProxy(boolean proxy) {
        super.setProxy(proxy);
    }

    void setServerChannel(Channel channel) {
        this.serverChannel = channel;
    }

    Channel getServerChannel() {
        return serverChannel;
    }

    BlockingQueue<ByteBuf> getMessageQueue() {
        return messageQueue;
    }

    @Override
    public ByteBuf getClientMessage() {
        try {
            return messageQueue.poll(1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public boolean sureMessageOnlyOne() {
        return false;
    }

    @Override
    protected void finalize() throws Throwable {
        ByteBuf buf;
        while ((buf = messageQueue.poll(1, TimeUnit.MILLISECONDS)) != null) {
            ReferenceCountUtil.release(buf);
        }
    }
}
