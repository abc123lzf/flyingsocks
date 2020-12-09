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
@Deprecated
public final class JksSSLEncryptProvider implements EncryptProvider {

    static final String NAME = "JKS";

    private SSLEngine sslEngine;

    private boolean initialize;

    private boolean client;

    JksSSLEncryptProvider() {
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public synchronized void initialize(Map<String, ?> params) throws Exception {
        if (initialize)
            throw new IllegalStateException("JksEncryptProvider instance has been initialize");
        client = (Boolean) params.get("client");
        sslEngine = buildSSLEngine(params);
        initialize = true;
    }

    @Override
    public boolean isInboundHandlerSameAsOutboundHandler() {
        return true;
    }

    @Override
    public ChannelInboundHandler decodeHandler(Map<String, ?> params) throws Exception {
        return createSSLHandler(params);
    }

    @Override
    public ChannelOutboundHandler encodeHandler(Map<String, ?> params) throws Exception {
        return createSSLHandler(params);
    }

    private SslHandler createSSLHandler(Map<String, ?> params) throws Exception {
        return new SslHandler(sslEngine);
    }

    private SSLEngine buildSSLEngine(Map<String, ?> params) throws Exception {
        if (params == null) {
            throw new NullPointerException("params should not be null.");
        }

        if (!params.containsKey("password") || !params.containsKey("url") || !params.containsKey("client")) {
            throw new IllegalArgumentException("Parameter key jksPass/jksUrl/isClient should not be null");
        }

        char[] pass = ((String) params.get("password")).toCharArray();

        URL url = new URL((String) params.get("url"));
        try (InputStream is = url.openStream()) {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(is, pass);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, pass);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), null, null);
            SSLEngine sslEngine = context.createSSLEngine();
            sslEngine.setUseClientMode(client);
            sslEngine.setNeedClientAuth(false);

            return sslEngine;
        }
    }
}
