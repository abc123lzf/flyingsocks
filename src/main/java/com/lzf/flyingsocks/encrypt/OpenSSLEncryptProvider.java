package com.lzf.flyingsocks.encrypt;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import java.io.InputStream;
import java.util.Map;

public class OpenSSLEncryptProvider implements EncryptProvider {
    static final String NAME = "OpenSSL";

    @Override
    public boolean isInboundHandlerSameAsOutboundHandler() {
        return true;
    }

    @Override
    public ChannelInboundHandler decodeHandler(Map<String, Object> params) throws Exception {
        return createSSLHandler(params);
    }

    @Override
    public ChannelOutboundHandler encodeHandler(Map<String, Object> params) throws Exception {
        return createSSLHandler(params);
    }

    private SslHandler createSSLHandler(Map<String, Object> params) throws Exception {
        boolean client = (boolean) params.get("client");
        if(client) {
            InputStream crt = (InputStream) params.get("file.cert");
            InputStream key = (InputStream) params.get("file.key");
            InputStream x509crt = (InputStream) params.get("file.cert.root");

            SslContext ctx = SslContextBuilder.forClient().keyManager(crt, key)
                    .trustManager(x509crt).build();

            return ctx.newHandler((ByteBufAllocator) params.get("alloc"));
        } else {
            InputStream crt = (InputStream) params.get("file.cert");
            InputStream key = (InputStream) params.get("file.key");
            InputStream x509crt = (InputStream) params.get("file.cert.root");
            SslContext ctx = SslContextBuilder.forServer(crt, key)
                    .trustManager(x509crt).clientAuth(ClientAuth.REQUIRE).build();

            if(params.get("alloc") == null)
                return ctx.newHandler(ByteBufAllocator.DEFAULT);
            return ctx.newHandler((ByteBufAllocator) params.get("alloc"));
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
