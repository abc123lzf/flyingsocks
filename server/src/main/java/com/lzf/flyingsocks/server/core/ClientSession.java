package com.lzf.flyingsocks.server.core;


import com.lzf.flyingsocks.AbstractSession;
import com.lzf.flyingsocks.Session;
import com.lzf.flyingsocks.protocol.DelimiterMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户端会话对象
 */
public class ClientSession extends AbstractSession implements Session {
    /**
     * 协商的协议分隔符
     */
    private byte[] delimiterKey;

    /**
     * 该客户端是否通过认证
     */
    private boolean auth = false;


    public ClientSession(Channel channel) {
        super((SocketChannel) channel);
    }


    public void setDelimiterKey(byte[] key) {
        if(delimiterKey == null) {
            if(key.length == DelimiterMessage.DEFAULT_SIZE)
                delimiterKey = Arrays.copyOf(key, DelimiterMessage.DEFAULT_SIZE);
            else
                throw new IllegalArgumentException(String.format("delimiter key only can set %d bytes",
                        DelimiterMessage.DEFAULT_SIZE));
        } else {
            throw new IllegalStateException("can not set double time delimiter key at same client.");
        }
    }

    public byte[] getDelimiterKey() {
        byte[] b = new byte[DelimiterMessage.DEFAULT_SIZE];
        System.arraycopy(delimiterKey, 0, b, 0, DelimiterMessage.DEFAULT_SIZE);
        return b;
    }

    public void writeMessage(ByteBuf msg) {
        checkChannelState();
        socketChannel.write(msg);
    }

    public void writeAndFlushMessage(ByteBuf msg) {
        checkChannelState();
        socketChannel.writeAndFlush(msg);
    }

    public void flushMessage() {
        checkChannelState();
        socketChannel.flush();
    }

    public boolean isAuth() {
        return auth;
    }

    public void passAuth() {
        auth = true;
    }

    public void updateLastActiveTime() {
        lastActiveTime = System.currentTimeMillis();
    }

    private void checkChannelState() throws IllegalStateException {
        if(!isActive())
            throw new IllegalStateException("Channel has been closed");
    }

    @Override
    public String toString() {
        return "ClientSession{" +
                "delimiterKey=" + Arrays.toString(delimiterKey) +
                ", auth=" + auth +
                '}';
    }
}
