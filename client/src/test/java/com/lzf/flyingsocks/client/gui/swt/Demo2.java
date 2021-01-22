package com.lzf.flyingsocks.client.gui.swt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Demo2 {

    public static void main(String[] args) throws Exception {
        ByteBuf buf = Unpooled.directBuffer(10);
        System.out.println(Long.toHexString(buf.memoryAddress()));
    }
}
