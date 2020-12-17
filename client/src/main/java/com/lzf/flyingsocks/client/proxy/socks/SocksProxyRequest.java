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

    /**
     * 消息队列
     */
    private final BlockingQueue<ByteBuf> messageQueue;

    /**
     * @param host 目标服务器主机名/IP
     * @param port 目标服务器
     * @param channel 客户端Channel通道
     */
    SocksProxyRequest(String host, int port, Channel channel) {
        super(Objects.requireNonNull(host), port, Objects.requireNonNull(channel), Protocol.TCP);
        messageQueue = new LinkedBlockingQueue<>();
    }

    @Override
    protected void setCtl(int ctl) {
        super.setCtl(ctl);
    }

    @Override
    protected void setCtl(int index, boolean mark) {
        super.setCtl(index, mark);
    }

    void setServerChannel(Channel channel) {
        assertChannel(channel, protocol);
        this.serverChannel = channel;
    }

    Channel serverChannel() {
        return serverChannel;
    }

    void tryTransferMessageQueue(ByteBuf message) {
        messageQueue.offer(message);
    }

    BlockingQueue<ByteBuf> messageQueue() {
        return messageQueue;
    }

    @Override
    protected void finalize() throws Throwable {
        ByteBuf buf;
        while ((buf = messageQueue.poll(1, TimeUnit.MILLISECONDS)) != null) {
            ReferenceCountUtil.release(buf);
        }
    }
}
