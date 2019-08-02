package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.GlobalConfig;

import java.io.*;
import java.net.URL;

public class OpenSSLConfig extends AbstractConfig {
    private static final String NAME_PREFIX = "config.OpenSSL.";

    public static final String CERT_FILE_NAME = "ca.crt";

    private final String host;

    private File rootCertFile;

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
        GlobalConfig cfg = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);
        this.rootCertFile = new File(new File(cfg.configPath(), host), CERT_FILE_NAME);
        if(!rootCertFile.exists())
            throw new FileNotFoundException("CA cert file at location " + rootCertFile.getAbsolutePath() + " doesn't exists.");
    }


    public InputStream openRootCertStream() throws IOException {
        return new FileInputStream(rootCertFile);
    }
}
