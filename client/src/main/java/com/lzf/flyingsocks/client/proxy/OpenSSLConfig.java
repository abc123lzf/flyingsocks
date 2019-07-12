package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.GlobalConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class OpenSSLConfig extends AbstractConfig {
    private static final String NAME_PREFIX = "config.OpenSSL.";

    public static final String CERT_FILE_NAME = "ca.crt";

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

        GlobalConfig cfg = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);
        File file = new File(new File(cfg.configLocation(), host), CERT_FILE_NAME);
        this.rootCertStream = new FileInputStream(file);
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
