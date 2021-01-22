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
package com.lzf.flyingsocks.encrypt;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class OpenSSLEncryptProvider implements EncryptProvider {
    private static final Logger log = LoggerFactory.getLogger("OpenSSLEncryptProvider");

    static final String NAME = "OpenSSL";

    private volatile SslContext sslContext;

    private boolean client;

    private boolean initialize = false;

    @Override
    public synchronized void initialize(Map<String, ?> params) throws Exception {
        if (initialize)
            throw new IllegalStateException("OpenSSLEncryptProvider instance has been initialize");
        if (!params.containsKey("client"))
            throw new IllegalArgumentException("params client must not be null and should be boolean type");

        client = (Boolean) params.get("client");
        sslContext = buildSSLContext(params);
        initialize = true;
    }

    @Override
    public boolean isInboundHandlerSameAsOutboundHandler() {
        return true;
    }

    @Override
    public ChannelInboundHandler decodeHandler(Map<String, ?> params) {
        if (!initialize)
            throw new IllegalStateException("OpenSSLEncryptProvider instance must be initial first !");
        return createSSLHandler(params);
    }

    @Override
    public ChannelOutboundHandler encodeHandler(Map<String, ?> params) {
        if (!initialize)
            throw new IllegalStateException("OpenSSLEncryptProvider instance must be initial first !");
        return createSSLHandler(params);
    }

    private SslHandler createSSLHandler(Map<String, ?> params) {
        if (params == null || params.get("alloc") == null)
            return sslContext.newHandler(ByteBufAllocator.DEFAULT);
        return sslContext.newHandler((ByteBufAllocator) params.get("alloc"));
    }

    private SslContext buildSSLContext(Map<String, ?> params) throws IOException {
        if (client) {
            if (params != null && params.containsKey("file.cert.root")) {
                try (InputStream x509crt = (InputStream) params.get("file.cert.root")) {
                    return SslContextBuilder.forClient().trustManager(x509crt).build();
                }
            } else {
                log.warn("No cert file provide, if your server's cert file signed by CA, you can ignore this warning.");
                return SslContextBuilder.forClient().build();
            }
        } else {
            try (InputStream crt = (InputStream) params.get("file.cert");  //该参数(签名请求)可为null
                 InputStream key = (InputStream) params.get("file.key");
                 InputStream x509crt = (InputStream) params.get("file.cert.root")) {
                return SslContextBuilder.forServer(crt, key)
                        .trustManager(x509crt).clientAuth(ClientAuth.NONE).build();
            }
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
