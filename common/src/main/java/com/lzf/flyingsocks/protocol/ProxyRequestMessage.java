package com.lzf.flyingsocks.protocol;

import com.lzf.flyingsocks.util.BaseUtils;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * 客户端向服务器发起的代理请求报文，其报文格式如下：
 * 0              4        5        5+clen   6+clen     8+clen
 * +--------------+--------+--------+--------+----------+-----------+
 * |  Serial ID   |Host Len|  Host  |  Type  |   Port   |Message Len|
 * |  (4 Bytes)   |(1 Byte)|        |(1 Byte)| (2 Bytes)| (4 Bytes) |
 * +--------------+--------+--------+--------+----------+-----------+
 * |                              Message                           |
 * |                              Content                           |
 * +----------------------------------------------------------------+
 * <p>
 * Serial ID：客户端消息序列号，用于识别代理消息从哪个应用程序发送
 * Host Len：目标主机名/IP地址长度
 * Host：目标主机名
 * Type：附加字段，当第7位为1时表示为UDP报文
 * Port：目标端口号
 * Message Len：消息长度
 * Message Content：消息体
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
        TCP, UDP, CLOSE; //Close表示要求服务器关闭与目标服务器的连接
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
        assertTrue(host != null && port > 0 && port <= 65535 && message != null,
                "ProxyRequestMessage is not complete, or port is illegal, message detail: \n" + toString());

        byte[] h = host.getBytes(HOST_ENCODING);

        int size = 4 + 1 + h.length + 1 + 2 + 4 + message.readableBytes();
        ByteBuf buf = getAllocator().buffer(size);

        buf.writeInt(serialId);

        buf.writeByte(h.length);
        buf.writeBytes(h);

        if (protocol == Protocol.UDP) {
            buf.writeByte(1 << 7);
        } else if (protocol == Protocol.CLOSE) {
            buf.writeByte(1 << 6);
        } else {
            buf.writeByte(0);
        }

        buf.writeShort(port);
        buf.writeInt(message.readableBytes());
        buf.writeBytes(message);

        return buf;
    }

    @Override
    public void deserialize(ByteBuf buf) throws SerializationException {
        try {
            int sid = buf.readInt();
            int hostlen = BaseUtils.parseByteToInteger(buf.readByte());
            byte[] hb = new byte[hostlen];

            buf.readBytes(hb);
            String host = new String(hb, HOST_ENCODING);

            byte ctl = buf.readByte();
            if ((ctl & (1 << 7)) != 0) {
                protocol = Protocol.UDP;
            } else if ((ctl & (1 << 6)) != 0) {
                protocol = Protocol.CLOSE;
            } else {
                protocol = Protocol.TCP;
            }

            int port = BaseUtils.parseUnsignedShortToInteger(buf.readShort());
            int msglen = buf.readInt();

            assertTrue(port > 0 && port <= 65535, "Illegal ProxyRequestMessage, port should between 1 and 65535");
            assertTrue(msglen >= 0, "Illegal ProxyRequestMessage, msglen should be greater than 0");
            assertTrue(msglen <= buf.readableBytes(), "Illegal ProxyRequestMessage, msglen is not equals real message length");

            ByteBuf msg = getAllocator().buffer(msglen);
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
