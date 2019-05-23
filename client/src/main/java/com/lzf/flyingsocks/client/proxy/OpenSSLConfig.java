package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;

import java.io.InputStream;
import java.net.URL;

public class OpenSSLConfig extends AbstractConfig {
    static final String NAME = "config.OpenSSL";

    private final String host;

    private InputStream certStream;

    private InputStream keyStream;

    private InputStream rootCertStream;

    public OpenSSLConfig(ConfigManager<?> configManager, String host) {
        super(configManager, NAME);
        this.host = host;
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        try {
            loadResource();
        } catch (Exception e) {
            throw new ConfigInitializationException(e);
        }
    }

    private synchronized void loadResource() throws Exception {
        if(certStream != null && keyStream != null && rootCertStream != null)
            return;
        URL root = new URL(String.format("classpath://encrypt/%s/ca.crt", host));
        this.rootCertStream = root.openStream();
        /*URL cert = new URL(String.format("classpath://encrypt/%s/client.csr", host));
        this.certStream = cert.openStream();
        URL key = new URL(String.format("classpath://encrypt/%s/pkcs8_client.key", host));
        this.keyStream = key.openStream();*/
    }

    /*public InputStream openClientCertStream() {
        if(certStream == null) {
            try {
                loadResource();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        InputStream is = certStream;
        certStream = null;
        return is;
    }

    public InputStream openKeyStream() {
        if(keyStream == null) {
            try {
                loadResource();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        InputStream is = keyStream;
        keyStream = null;
        return is;
    }*/

    public InputStream openRootCertStream() {
        if(rootCertStream == null) {
            try {
                loadResource();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        InputStream is = rootCertStream;
        rootCertStream = null;
        return is;
    }
}
