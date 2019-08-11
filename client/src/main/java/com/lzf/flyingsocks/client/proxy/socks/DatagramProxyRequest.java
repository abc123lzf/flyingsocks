package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.client.proxy.ProxyRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramChannel;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * UDP代理请求
 */
public final class DatagramProxyRequest extends ProxyRequest {

    /**
     * 来自客户端，去除包头的UDP消息
     */
    private ByteBuf content;

    /**
     * UDP包的发送方
     */
    private final InetSocketAddress sender;

    DatagramProxyRequest(String ipAddr, int port, DatagramChannel channel, InetSocketAddress sender) {
        super(Objects.requireNonNull(ipAddr), port, Objects.requireNonNull(channel), Protocol.UDP);
        this.sender = Objects.requireNonNull(sender);
    }

    void setContent(ByteBuf content) {
        this.content = content;
    }

    public InetSocketAddress senderAddress() {
        return sender;
    }

    @Override
    public DatagramChannel clientChannel() {
        return (DatagramChannel)super.clientChannel();
    }

    @Override
    public ByteBuf takeClientMessage() {
        if(content != null) {
            ByteBuf buf = content;
            content = null;
            return buf;
        }
        return null;
    }

    @Override
    public boolean ensureMessageOnlyOne() {
        return true;
    }

    @Override
    public String toString() {
        return "DatagramProxyRequest{" +
                "sender=" + sender +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", protocol=" + protocol +
                '}';
    }
}
