package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class OpenSSLConfig extends AbstractConfig {
    /**
     * 默认配置名
     */
    static final String NAME = "config.OpenSSL";

    /**
     * 证书文件URL,如果是PEM格式直接重命名为CRT格式
     */
    private URL rootCertURL;

    /**
     * PCKS8编码的私钥文件
     */
    private URL privateKeyURL;

    public OpenSSLConfig(ConfigManager<?> configManager) {
        super(configManager, NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        try {
            this.rootCertURL = new URL("classpath://encrypt/ca.crt");
            this.privateKeyURL = new URL("classpath://encrypt/private.key");
        } catch (Exception e) {
            throw new ConfigInitializationException(e);
        }
    }

    /**
     * @return 打开一个私钥文件输入流，使用完成后请手动关闭
     */
    public InputStream openKeyStream() throws IOException {
        return privateKeyURL.openStream();
    }

    /**
     * @return 打开一个证书文件输入流，使用完成后请手动关闭
     */
    public InputStream openRootCertStream() throws IOException {
        return rootCertURL.openStream();
    }
}
