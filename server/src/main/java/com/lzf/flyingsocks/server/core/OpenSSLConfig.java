package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;

import java.io.InputStream;
import java.net.URL;

public class OpenSSLConfig extends AbstractConfig {
    static final String NAME = "config.OpenSSL";

    private InputStream certStream;

    private InputStream keyStream;

    private InputStream rootCertStream;

    public OpenSSLConfig(ConfigManager<?> configManager) {
        super(configManager, NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        try {
            URL root = new URL("classpath://encrypt/ca.crt");
            this.rootCertStream = root.openStream();
            URL cert = new URL("classpath://encrypt/server.crt");
            this.certStream = cert.openStream();
            URL key = new URL("classpath://encrypt/pkcs8_server.key");
            this.keyStream = key.openStream();
        } catch (Exception e) {
            throw new ConfigInitializationException(e);
        }
    }

    public InputStream openServerCertStream() {
        return certStream;
    }

    public InputStream openKeyStream() {
        return keyStream;
    }

    public InputStream openRootCertStream() {
        return rootCertStream;
    }
}
