package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;

import java.io.InputStream;
import java.net.URL;

public class OpenSSLConfig extends AbstractConfig {
    private static final String NAME_PREFIX = "config.OpenSSL.";

    private final String host;

    private InputStream rootCertStream;

    public OpenSSLConfig(ConfigManager<?> configManager, String host) {
        super(configManager, NAME_PREFIX + host);
        this.host = host;
    }

    public static String generalName(String host) {
        return NAME_PREFIX + host;
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
        if(rootCertStream != null)
            return;
        URL root = new URL(String.format("classpath://encrypt/%s/ca.crt", host));
        this.rootCertStream = root.openStream();
    }


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
