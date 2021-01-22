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
package com.lzf.flyingsocks.server.core.client;

import com.lzf.flyingsocks.protocol.CertRequestMessage;
import com.lzf.flyingsocks.protocol.CertResponseMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/15 5:33
 */
@ChannelHandler.Sharable
class CertRequestHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(CertRequestHandler.class);

    /**
     * 证书文件缓存
     */
    private final byte[] cert;

    /**
     * 证书文件MD5
     */
    private final byte[] certMd5;

    /**
     * 用于决策认证是否通过
     */
    private final Predicate<CertRequestMessage> authPredicate;


    public CertRequestHandler(byte[] cert, Predicate<CertRequestMessage> authPredicate) {
        this.cert = Arrays.copyOf(cert, cert.length);

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            this.certMd5 = md.digest(cert);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }

        this.authPredicate = Objects.requireNonNull(authPredicate);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            return;
        }

        log.warn("An exception occur in CertRequestHandler", cause);
        ctx.close();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            try {
                channelRead0(ctx, (ByteBuf) msg);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        CertRequestMessage req = new CertRequestMessage(msg);
        boolean auth = authPredicate.test(req);
        if (!auth) {
            ctx.close();
            return;
        }

        byte[] src = req.getCertMD5();
        if (!Arrays.equals(certMd5, src)) {
            CertResponseMessage resp = new CertResponseMessage(true, cert);
            ctx.writeAndFlush(resp, ctx.voidPromise());
        } else {
            CertResponseMessage resp = new CertResponseMessage(false, null);
            ctx.writeAndFlush(resp, ctx.voidPromise());
        }
    }

}
