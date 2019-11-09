package com.lzf.flyingsocks.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

/**
 * @author lzf abc123lzf@126.com
 * @since 2019/11/2 2:05
 */
public class ByteBufTestUtils {

    public static void printDatagramPacket(Logger log, DatagramPacket packet) {
        InetSocketAddress from = packet.sender();
        InetSocketAddress to = packet.recipient();
        log.debug("To: {}:{}   From: {}:{}", to.getHostName(), to.getPort(), from.getHostName(), from.getPort());
        log.debug("");

        ByteBuf buf = packet.content();
        byte[] b = new byte[buf.readableBytes()];
        buf.readBytes(b);

        StringBuilder sb = new StringBuilder();
    }

    private ByteBufTestUtils() {
        throw new UnsupportedOperationException();
    }

}
