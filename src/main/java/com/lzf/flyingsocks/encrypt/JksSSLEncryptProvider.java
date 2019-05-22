package com.lzf.flyingsocks.encrypt;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.Map;

/**
 * 提供基于JKS的SSL加密连接
 */
@SuppressWarnings("unused")
public final class JksSSLEncryptProvider implements EncryptProvider {

    static final String NAME = "JKS";

    JksSSLEncryptProvider() { }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isInboundHandlerSameAsOutboundHandler() {
        return true;
    }

    @Override
    public ChannelInboundHandler decodeHandler(Map<String, String> params) throws Exception {
        return createSSLHandler(params);
    }

    @Override
    public ChannelOutboundHandler encodeHandler(Map<String, String> params) throws Exception {
        return createSSLHandler(params);
    }

    private SslHandler createSSLHandler(Map<String, String> params) throws Exception {
        if(params == null)
            throw new NullPointerException("params should not be null.");

        if(!params.containsKey("jksPass") || !params.containsKey("jksUrl") || !params.containsKey("isClient"))
            throw new IllegalArgumentException("Parameter key jksPass/jksUrl/isClient should not be null");

        String cli = params.get("isClient").toLowerCase();
        if(!cli.equals("true") && !cli.equals("false")) {
            throw new IllegalArgumentException("Parameter key isClient value should be true or false");
        }

        char[] pass = params.get("jksPass").toCharArray();

        URL url = new URL(params.get("jksUrl"));
        try(InputStream is = url.openStream()) {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(is, pass);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, pass);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), null, null);
            SSLEngine sslEngine = context.createSSLEngine();
            sslEngine.setUseClientMode(Boolean.valueOf(cli));
            sslEngine.setNeedClientAuth(false);

            return new SslHandler(sslEngine);
        }
    }
}
