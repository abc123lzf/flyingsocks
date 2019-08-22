package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.AbstractSession;
import com.lzf.flyingsocks.protocol.DelimiterMessage;
import io.netty.channel.socket.SocketChannel;

import java.util.Arrays;

public class ProxyServerSession extends AbstractSession {

    private byte[] delimiter;

    ProxyServerSession(SocketChannel serverChannel) {
        super(serverChannel);
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public void setDelimiter(byte[] delimiter) {
        if(delimiter == null || delimiter.length != DelimiterMessage.DEFAULT_SIZE)
            throw new IllegalArgumentException("Delimiter length must be " + DelimiterMessage.DEFAULT_SIZE + " bytes");
        this.delimiter = Arrays.copyOf(delimiter, DelimiterMessage.DEFAULT_SIZE);
    }

    public byte[] getDelimiter() {
        if(delimiter == null)
            return null;
        return Arrays.copyOf(delimiter, delimiter.length);
    }
}
