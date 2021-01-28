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
package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.client.proxy.util.MessageDeliverer;
import com.lzf.flyingsocks.client.proxy.util.MessageReceiver;
import com.lzf.flyingsocks.protocol.ProxyRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

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


    public Channel clientChannel() {
        return clientChannel;
    }


    public void addClientChannelCloseListener(GenericFutureListener<? extends Future<? super Void>> listener) {
        clientChannel.closeFuture().addListener(listener);
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
