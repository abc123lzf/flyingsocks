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
package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.util.BaseUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.nio.charset.Charset;
import java.util.List;

/**
 * 处理Socks5的UDP穿透请求
 * 请求格式如下(RFC1928)：
 *
 * +----+------+------+----------+----------+----------+
 * |RSV | FRAG | ATYP | DST.ADDR | DST.PORT |   DATA   |
 * +----+------+------+----------+----------+----------+
 * | 2  |  1   |   1  | Variable |     2    | Variable |
 * +----+------+------+----------+----------+----------+
 * o RSV Reserved X’0000’
 * o FRAG Current fragment number
 * o ATYP address type of following addresses:
 *   IP V4 address: X’01’
 *   DOMAINNAME: X’03’
 *   IP V6 address: X’04’
 * o DST.ADDR desired destination address
 * o DST.PORT desired destination port
 * o DATA user data
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/11 3:57
 */
class UdpProxyMessageDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private static final Charset UNICODE = Charset.forName("Unicode");

    private final ReassemblyQueue reassemblyQueue = new ReassemblyQueue();

    @Override
    protected final void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {
        ByteBuf buf = packet.content();
        short rsv = buf.readShort();
        if (rsv != 0) {
            throw new DecoderException("Illegal field RSV");
        }

        byte frag = buf.readByte();
        byte atyp = buf.readByte();

        String dstAddr = obtainTargetHost(buf, atyp);
        int dstPort = buf.readUnsignedShort();

        ByteBuf data = ctx.alloc().buffer(buf.readableBytes());
        buf.readBytes(data);

        if (frag == 0) {
            out.add(new UdpProxyMessage(dstAddr, dstPort, data));
            return;
        }

        UdpProxyMessage msg = reassemblyQueue.tryAppendAndGet(frag, new UdpProxyMessage(dstAddr, dstPort, data));
        if (msg != null) {
            out.add(msg);
        }
    }


    private String obtainTargetHost(ByteBuf buf, byte atyp) {
        switch (atyp) {
            case 0x01:
                return BaseUtils.parseIntToIPv4Address(buf.readInt());
            case 0x03:
                short len = buf.readUnsignedByte();
                byte[] host = new byte[len];
                buf.readBytes(host);
                return new String(host, UNICODE);
            case 0x04:
                byte[] ipv6 = new byte[16];
                buf.readBytes(ipv6);
                return BaseUtils.parseByteArrayToIPv6Address(ipv6);
        }

        throw new DecoderException("Illegal field ATYP");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        reassemblyQueue.reset();
        super.channelInactive(ctx);
    }
}

