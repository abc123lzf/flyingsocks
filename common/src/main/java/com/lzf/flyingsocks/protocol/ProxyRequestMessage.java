package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * 客户端向服务器发起的代理请求报文，其报文格式如下：
 * 0              16        Clen+16
 * +--------------+---------+---------+----------+-----------+
 * |  Serial ID   |Host Len |   Host  | Port/Ctr |Message Len|
 * |  (4 Bytes)   |(2 Bytes)|         | (4 Bytes)| (4 Bytes) |
 * +--------------+---------+---------+----------+-----------+
 * |                              Message                    |
 * |                              Content                    |
 * +---------------------------------------------------------+
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

    /**
     * 传输层协议
     */
    private Protocol protocol;


    public enum Protocol {
        TCP, UDP
    }


    public ProxyRequestMessage(int serialId, Protocol protocol) {
        super(serialId);
        this.protocol = Objects.requireNonNull(protocol);
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

    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    public ByteBuf serialize() throws SerializationException {
        if(host == null || port <= 0 || port >= 65535 || message == null)
            throw new SerializationException("ProxyRequestMessage is not complete, or port is illegal, message detail: \n" + toString());

        byte[] h = host.getBytes(HOST_ENCODING);

        int size = 4 + 2 + h.length + 4 + 4 + message.readableBytes();
        ByteBuf buf = Unpooled.buffer(size);

        buf.writeInt(serialId);

        buf.writeShort(h.length);
        buf.writeBytes(h);

        if(protocol == Protocol.UDP)
            buf.writeInt(1 << 31 | port);  //端口字段首位为1表示UDP协议
        else
            buf.writeInt(port);

        buf.writeInt(message.readableBytes());
        buf.writeBytes(message);

        return buf;
    }

    @Override
    public void deserialize(ByteBuf buf) throws SerializationException {
        try {
            int sid = buf.readInt();
            short hostlen = buf.readShort();
            byte[] hb = new byte[hostlen];

            buf.readBytes(hb);
            String host = new String(hb, HOST_ENCODING);

            int val = buf.readInt();

            int port;
            if(val < 0) {  //最高位为1代表为UDP协议
                port = val ^ (1 << 31);
                protocol = Protocol.UDP;
            } else {
                port = val;
                protocol = Protocol.TCP;
            }

            int msglen = buf.readInt();

            if(port <= 0 || port >= 65535)
                throw new SerializationException("Illegal ProxyRequestMessage, port should between 1 and 65534");

            if(msglen < 0)
                throw new SerializationException("Illegal ProxyRequestMessage, msglen should be greater than 0");

            if (msglen > buf.readableBytes())
                throw new SerializationException("Illegal ProxyRequestMessage, msglen is not equals real message length");

            ByteBuf msg = Unpooled.buffer(msglen);
            buf.readBytes(msg);

            this.serialId = sid;
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
                "host='" + host + '\'' +
                ", port=" + port +
                ", protocol=" + protocol +
                '}';
    }
}
