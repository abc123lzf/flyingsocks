package com.lzf.flyingsocks.client.gui.swt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

public class Demo2 {

    public static void main(String[] args) throws Exception {
        ByteBuf buf = Unpooled.buffer(100);
        ByteBuf buf2 = Unpooled.buffer(20);

        CompositeByteBuf res = Unpooled.compositeBuffer(2);
        res.addComponent(true, buf);
        res.addComponent(true, buf2);

        System.out.println(buf.refCnt() + " " + buf2.refCnt() + " " + res.refCnt());
        res.release();
        System.out.println(buf.refCnt() + " " + buf2.refCnt() + " " + res.refCnt());
    }
}
