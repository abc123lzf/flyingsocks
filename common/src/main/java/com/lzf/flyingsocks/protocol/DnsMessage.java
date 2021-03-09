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
package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;

/**
 * DNS解析请求，解决国内域名解析被污染的问题
 * 目前用于C语言编写的路由器客户端
 * https://www.jianshu.com/p/36280f055df3
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/3/4 17:44
 */
public abstract class DnsMessage extends ServiceStageMessage {

    /**
     * 服务ID
     */
    public static final byte SERVICE_ID = 0x01;

    /**
     * flag字段中的RCODE，表示成功
     */
    public static final byte RCODE_SUCCESS = 0;

    /**
     * 格式错误，服务器不能理解请求的报文格式。	[RFC1035]
     */
    public static final byte RCODE_FORMAT_ERROR = 1;

    /**
     * 服务器失败，因为服务器的原因导致没办法处理这个请求。	[RFC1035]
     */
    public static final byte RCODE_SERVER_FAILED = 2;

    /**
     * 名字错误，该值只对权威应答有意义，它表示请求的域名不存在。	[RFC1035]
     */
    public static final byte RCODE_NOT_EXISTS = 3;

    /**
     * 未实现，域名服务器不支持该查询类型。	[RFC1035]
     */
    public static final byte RCODE_NOT_IMPL = 4;

    /**
     * 拒绝服务，服务器由于设置的策略拒绝给出应答。比如，服务器不希望对个请求者给出应答时可以使用此响应码。
     */
    public static final byte RCODE_REFUSE = 5;


    /**
     * DNS头部字段大小
     */
    private static final int HEADER_SIZE = 12;

    /**
     * 事务ID
     */
    protected short transactionId;

    /**
     * FLAGS
     */
    protected short flags;

    /**
     * QUESTION数量
     */
    protected short questionCount;

    /**
     * ANSWER数量
     */
    protected short answerCount;

    /**
     * 授权数目
     */
    protected short authorityCount;

    /**
     * 附加信息数量
     */
    protected short additionalInfomationCount;



    public static class Question {

        /**
         * 一般为域名
         */
        private final String name;

        /**
         * @see io.netty.handler.codec.dns.DnsRecordType
         */
        private final short type;

        /**
         * @see io.netty.handler.codec.dns.DnsRecord#CLASS_IN
         */
        private final short klass;

        public Question(String name, short type, short klass) {
            this.name = name;
            this.type = type;
            this.klass = klass;
        }

        public String getName() {
            return name;
        }

        public short getType() {
            return type;
        }

        public short getKlass() {
            return klass;
        }
    }


    public static class Record {

        private final String domain;

        private final short type;

        private final short klass;

        private final int ttl;

        private final short dataLength;

        private final byte[] data;

        public Record(String domain, short type, short klass, int ttl, short dataLength, byte[] data) {
            this.domain = domain;
            this.type = type;
            this.klass = klass;
            this.ttl = ttl;
            this.dataLength = dataLength;
            this.data = data;
        }

        public String getDomain() {
            return domain;
        }

        public short getType() {
            return type;
        }

        public short getKlass() {
            return klass;
        }

        public int getTTL() {
            return ttl;
        }

        public short getDataLength() {
            return dataLength;
        }

        public byte[] getData() {
            return data;
        }
    }


    protected DnsMessage(short transactionId) {
        super(SERVICE_ID);
        this.transactionId = transactionId;
    }

    protected DnsMessage(ByteBuf buf) throws SerializationException {
        super(buf);
    }

    /**
     * QR是1bit flag位，值为0表示这是一次查询，1则表示该报文是响应报文
     */
    protected void setQR(boolean open) {
        short flag = this.flags;
        if (open) {
            flag |= 1;
        } else {
            flag &= 0xFFFE;
        }
        this.flags = flag;
    }

    /**
     * 4bit长的查询类型字段，这个值由查询发起者设置
     *
     * 0	 Query（最常用的查询）	[RFC1035]
     * 1	 IQuery (反向查询，现在已经不再使用了)	[RFC3425]
     * 2	 Status	[RFC1035]
     * 3	 未指定
     * 4	 Notify	[RFC1996]
     * 5	 Update	[RFC2136]
     * 6	 DNS Stateful Operations (DSO)
     * 7-15  Unused
     */
    protected void setOpcode(byte code) {
        code &= 0xF;
        code <<= 1;
        short flag = this.flags;
        flag &= 0xFFE1;
        flag |= code;
        this.flags = flag;
    }

    /**
     * 1bit权威应答标记，当响应报文由权威服务器发出时，该位置1，否则为0。
     */
    protected void setAA(boolean open) {
        short flag = this.flags;
        if (open) {
            flag |= (1 << 5);
        } else {
            flag &= 0xFFDF;
        }
        this.flags = flag;
    }

    /**
     * 当使用UDP传输时，若响应数据超过DNS标准限制（超过512B），数据包便会发生截断，
     * 超出部分被丢弃，此时该flag位被置1。
     */
    protected void setTC(boolean open) {
        short flag = this.flags;
        if (open) {
            flag |= (1 << 6);
        } else {
            flag &= 0xFFBF;
        }
        this.flags = flag;
    }

    /**
     * 客户端希望服务器对此次查询进行递归查询时将该位置1，否则置0。响应时RD位会复制到响应报文内。
     */
    protected void setRD(boolean open) {
        short flag = this.flags;
        if (open) {
            flag |= (1 << 7);
        } else {
            flag &= 0xFF7F;
        }
        this.flags = flag;
    }

    /**
     * 服务器根据自己是否支持递归查询对该位进行设置。1为支持递归查询，0为不支持递归查询。
     */
    protected void setRA(boolean open) {
        short flag = this.flags;
        if (open) {
            flag |= (1 << 8);
        } else {
            flag &= 0xFEFF;
        }
        this.flags = flag;
    }

    /**
     * RCODE(Response Code)响应码
     *
     * @see DnsMessage#RCODE_SUCCESS
     * @see DnsMessage#RCODE_FORMAT_ERROR
     * @see DnsMessage#RCODE_SERVER_FAILED
     * @see DnsMessage#RCODE_NOT_IMPL
     * @see DnsMessage#RCODE_REFUSE
     */
    protected void setRCODE(byte code) {
        short c = (short) (((short) (code & 0xF)) << 12);
        short flag = this.flags;
        flag &= 0x0FFF;
        flag |= c;
        this.flags = flag;
    }


    @Override
    protected final ByteBuf serialize0(ByteBufAllocator allocator) throws SerializationException {
        ByteBuf header = allocator.directBuffer(HEADER_SIZE);
        header.writeShort(transactionId);
        header.writeShortLE(flags);
        header.writeShort(questionCount);
        header.writeShort(answerCount);
        header.writeShort(authorityCount);
        header.writeShort(additionalInfomationCount);

        ByteBuf body;
        try {
            body = serializeBody(allocator);
        } catch (RuntimeException e) {
            header.release();
            throw new SerializationException(getClass(), e);
        }

        CompositeByteBuf result = allocator.compositeBuffer(2);
        result.addComponent(true, header);
        result.addComponent(true, body);
        return result;
    }

    /**
     * 序列化DNS非头部字段
     */
    protected abstract ByteBuf serializeBody(ByteBufAllocator allocator) throws SerializationException;


    @Override
    protected final void deserialize0(ByteBuf buf) throws SerializationException {
        if (buf.readableBytes() < HEADER_SIZE) {
            throw new SerializationException(getClass(), "Header size too small: " + buf.readableBytes());
        }

        this.transactionId = buf.readShort();
        this.flags = buf.readShortLE();
        this.questionCount = buf.readShort();
        this.answerCount = buf.readShort();
        this.authorityCount = buf.readShort();
        this.additionalInfomationCount = buf.readShort();
        deserializeBody(buf);
    }


    /**
     * 反序列化DNS非头部字段
     */
    protected abstract void deserializeBody(ByteBuf buf) throws SerializationException;


    public short getTransactionId() {
        return transactionId;
    }


    protected static Question readQuestion(ByteBuf buf) {
        StringBuilder sb = new StringBuilder(15);
        byte b;
        while ((b = buf.readByte()) != 0) {
            sb.append((char) b);
        }

        short type = buf.readShort();
        short klass = buf.readShort();

        return new Question(sb.toString(), type, klass);
    }


    protected static Record readResource(ByteBuf buf) {
        StringBuilder sb = new StringBuilder(15);
        byte b;
        while ((b = buf.readByte()) != 0) {
            sb.append((char) b);
        }

        short type = buf.readShort();
        short klass = buf.readShort();
        int ttl = buf.readInt();
        short dataLength = buf.readShort();
        byte[] data = new byte[dataLength];
        buf.readBytes(data);

        return new Record(sb.toString(), type, klass, ttl, dataLength, data);
    }
}
