package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.client.proxy.util.MessageDeliverer;
import com.lzf.flyingsocks.client.proxy.util.MessageReceiver;
import com.lzf.flyingsocks.protocol.ProxyRequestMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;

import java.io.IOException;

/**
 * 代理请求
 */
public class ProxyRequest implements Comparable<ProxyRequest>, Cloneable {

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
     * 客户端消息 -> 接收者传递工具/缓存
     */
    protected final MessageDeliverer clientMessageDeliverer = new MessageDeliverer();

    /**
     * 代理协议
     */
    protected final Protocol protocol;

    /**
     * 附加字段
     */
    protected int ctl;

    /**
     * 是否进行代理
     */
    protected boolean proxy;

    /**
     * 该Request是否已经关闭
     */
    protected volatile boolean close = false;


    /**
     * 代理协议枚举，目前支持UDP、TCP
     */
    public enum Protocol {
        TCP, UDP;

        public ProxyRequestMessage.Protocol toMessageType() {
            if (this == TCP) {
                return ProxyRequestMessage.Protocol.TCP;
            } else {
                return ProxyRequestMessage.Protocol.UDP;
            }
        }
    }


    public ProxyRequest(String host, int port, Channel clientChannel, Protocol protocol) {
        this.host = host;
        this.port = port;
        assertChannel(clientChannel, protocol);
        this.clientChannel = clientChannel;
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

    /**
     * 判断ctl字段某一位是否被标记为1
     *
     * @param index 位数
     * @return 是否被标记为1
     */
    public final boolean isCtlMark(int index) {
        int c = ctl;
        return (c & (1 << index)) > 0;
    }

    public final int getCtl() {
        return ctl;
    }

    public final boolean needProxy() {
        return proxy;
    }

    protected void setCtl(int ctl) {
        this.ctl = ctl;
    }

    /**
     * 将ctl字段某一位标为0或1
     *
     * @param index 位数，从0开始
     * @param mark  true标为1 false标为0
     */
    protected void setCtl(int index, boolean mark) {
        int c = ctl;
        if (mark) {
            c |= (1 << index);
        } else {
            c &= (~(1 << index));
        }

        this.ctl = c;
    }

    public Channel clientChannel() {
        return clientChannel;
    }

    public void closeClientChannel() {
        if (clientChannel.isActive()) {
            clientChannel.close();
        }
    }

    /**
     * @param proxy 是否需要走代理通道
     */
    protected void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    /**
     * 设置客户端消息ByteBuf接收者
     *
     * @param receiver 消息接收者
     * @throws IOException 如果ClientMessageDeliverer已经cancel
     */
    public void setClientMessageReceiver(MessageReceiver receiver) throws IOException {
        clientMessageDeliverer.setReceiver(receiver);
    }

    /**
     * 传递或者暂时缓存客户端的消息
     *
     * @param message 来自客户端的{@link ByteBuf}
     * @throws IOException 如果ClientMessageDeliverer已经cancel
     */
    public void transferClientMessage(ByteBuf message) throws IOException {
        clientMessageDeliverer.transfer(message);
    }

    /**
     * @return 本次代理请求是否已经关闭或者废除
     */
    public boolean isClose() {
        return close;
    }


    /**
     * 关闭/废除本次代理请求
     */
    public void close() {
        if (close) {
            return;
        }

        synchronized (this) {
            if (close) {
                return;
            }

            close = true;
            clientMessageDeliverer.cancel();

            if (clientChannel.isActive()) {
                clientChannel.close();
            }
        }
    }


    @Override
    public final boolean equals(Object obj) {
        ProxyRequest req;
        return this == obj || (obj instanceof ProxyRequest &&
                this.clientChannel.id().equals((req = (ProxyRequest) obj).clientChannel.id()) &&
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
        if (this.equals(request)) {
            return 0;
        }

        if (this.hashCode() > request.hashCode())
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
            case TCP:
                assert channel instanceof SocketChannel;
                break;
            case UDP:
                assert channel instanceof DatagramChannel;
                break;
            default:
                throw new AssertionError("Illegal protocol");
        }
    }
}
