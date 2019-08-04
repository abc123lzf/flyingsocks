package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.protocol.ProxyRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;

/**
 * 代理请求
 */
public abstract class ProxyRequest implements Comparable<ProxyRequest>, Cloneable {

    /**
     * 目标服务器
     */
    protected final String host;

    /**
     * 目标服务器端口
     */
    protected final int port;

    /**
     * 客户端Channel通道
     */
    protected final Channel clientChannel;

    /**
     * 代理协议
     */
    protected final Protocol protocol;

    /**
     * 代理协议枚举，目前支持UDP、TCP
     */
    public enum Protocol {
        TCP, UDP;

        public ProxyRequestMessage.Protocol toMessageType() {
            if(this == TCP)
                return ProxyRequestMessage.Protocol.TCP;
            else
                return ProxyRequestMessage.Protocol.UDP;
        }
    }


    protected ProxyRequest(String host, int port, Channel channel, Protocol protocol) {
        this.host = host;
        this.port = port;
        assertChannel(channel, protocol);
        this.clientChannel = channel;
        this.protocol = protocol;
    }

    public final String getHost() {
        return host;
    }

    public final int getPort() {
        return port;
    }

    public final Protocol protocol() {
        return protocol;
    }

    protected Channel clientChannel() {
        return clientChannel;
    }

    protected void closeClientChannel() {
        if(clientChannel.isActive())
            clientChannel.close();
    }

    /**
     * 获取客户端发送的请求信息
     * @return 字节容器
     */
    public abstract ByteBuf takeClientMessage();

    /**
     * @return 是否确定客户端发送的消息仅有一条，例如调用
     * 一次getClientMessage()方法后不可能再次调用getClientMessage()方法后获取到ByteBuf实例
     */
    public abstract boolean ensureMessageOnlyOne();


    @Override
    public final boolean equals(Object obj) {
        ProxyRequest req;
        return this == obj || (obj instanceof ProxyRequest &&
                this.clientChannel.id().equals((req = (ProxyRequest)obj).clientChannel.id()) &&
                this.host.equals(req.host) &&
                this.port == req.port);
    }

    @Override
    public final int hashCode() {
        return clientChannel.id().hashCode() ^ host.hashCode();
    }

    @Override
    public String toString() {
        return "ProxyRequest [host:" + host + ", port:" + port + "]";
    }

    @Override
    public int compareTo(ProxyRequest request) {
        if(this.equals(request))
            return 0;

        if(this.hashCode() > request.hashCode())
            return 1;
        else
            return -1;
    }

    @Override
    protected ProxyRequest clone() throws CloneNotSupportedException {
        return (ProxyRequest) super.clone();
    }

    protected static void assertChannel(Channel channel, Protocol protocol) {
        switch (protocol) {
            case TCP: assert channel instanceof SocketChannel; break;
            case UDP: assert channel instanceof DatagramChannel; break;
            default:
                throw new AssertionError("Illegal protocol");
        }
    }
}
