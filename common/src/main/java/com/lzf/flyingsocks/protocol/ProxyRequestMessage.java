package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.Charset;

/**
 * 客户端向服务器发起的代理请求报文，其报文格式如下：
 * 0              16        Clen+16
 * +--------------+-----------+---------+---------+----------+-----------+
 * |Channel ID Len| Channel ID|Host Len |   Host  | Port/Ctr |Message Len|
 * |  (2 Bytes)   |           |(2 Bytes)|         | (4 Bytes)| (4 Bytes) |
 * +--------------+-----------+---------+---------+----------+-----------+
 * |                              Message                                |
 * |                              Content                                |
 * +---------------------------------------------------------------------+
 */
public class ProxyRequestMessage extends ProxyMessage implements Message, Cloneable {

    private static final Charset HOST_ENCODING = Charset.forName("Unicode");

    /**
     * 代理主机名，例如www.google.com
     */
    private String host;

    /**
     * 端口号，例如80、443
     */
    private int port;


    public ProxyRequestMessage(String channelId) {
        super(channelId);
    }

    public ProxyRequestMessage(ByteBuf buf) throws SerializationException {
        super(buf);
    }

    /**
     * @return 代理目标主机名或者IP地址
     */
    public String getHost() {
        return host;
    }

    /**
     * @return 代理目标主机的端口号
     */
    public int getPort() {
        return port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setMessage(ByteBuf message) {
        this.message = message;
    }

    @Override
    public ByteBuf serialize() throws SerializationException {
        if(channelId == null || host == null || port <= 0 || port >= 65535 || message == null)
            throw new SerializationException("ProxyRequestMessage is not complete, or port is illegal, message detail: \n" + toString());

        byte[] cid = channelId.getBytes(CHANNEL_ENCODING);
        byte[] h = host.getBytes(HOST_ENCODING);

        int size = 2 + cid.length + 2 + h.length + 4 + 4 + message.readableBytes();
        ByteBuf buf = Unpooled.buffer(size);

        buf.writeShort(cid.length);
        buf.writeBytes(cid);

        buf.writeShort(h.length);
        buf.writeBytes(h);

        buf.writeInt(port);
        buf.writeInt(message.readableBytes());
        buf.writeBytes(message);

        return buf;
    }

    @Override
    public void deserialize(ByteBuf buf) throws SerializationException {
        short cidlen = buf.readShort();
        if(cidlen <= 0)
            throw new SerializationException("Illegal ProxyRequestMessage, client channel id length < 0");
        try {
            byte[] bid = new byte[cidlen];
            buf.readBytes(bid);
            String cid = new String(bid, CHANNEL_ENCODING);

            short hostlen = buf.readShort();
            byte[] hb = new byte[hostlen];

            buf.readBytes(hb);
            String host = new String(hb, HOST_ENCODING);

            int port = buf.readInt();
            int msglen = buf.readInt();

            if(port <= 0 || port >= 65535)
                throw new SerializationException("Illegal ProxyRequestMessage, port should between 1 and 65534");

            if(msglen < 0)
                throw new SerializationException("Illegal ProxyRequestMessage, msglen should be greater than 0");

            if (msglen > buf.readableBytes())
                throw new SerializationException("Illegal ProxyRequestMessage, msglen is not equals real message length");

            ByteBuf msg = Unpooled.buffer(msglen);
            buf.readBytes(msg);

            this.channelId = cid;
            this.host = host;
            this.port = port;
            this.message = msg;

        } catch (IndexOutOfBoundsException e) {
            throw new SerializationException("Illegal ProxyRequestMessage", e);
        }
    }

    @Override
    public ProxyRequestMessage clone() throws CloneNotSupportedException {
        ProxyRequestMessage obj = (ProxyRequestMessage) super.clone();
        obj.setMessage(message.copy());
        return obj;
    }

    @Override
    protected void finalize() {
        ReferenceCountUtil.release(message);
    }

    @Override
    public String toString() {
        return "ProxyRequestMessage{" +
                "channelId='" + channelId + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", message=" + message +
                '}';
    }
}
