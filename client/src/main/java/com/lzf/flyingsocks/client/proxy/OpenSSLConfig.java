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
package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.GlobalConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public class OpenSSLConfig extends AbstractConfig {
    private static final String NAME_PREFIX = "config.OpenSSL.";

    public static final String CERT_FILE_NAME = "ca.crt";

    private final String host;

    private final int port;

    private Path rootCertFile;

    OpenSSLConfig(ConfigManager<?> configManager, String host, int port) {
        super(configManager, generalName(host, port));
        this.host = host;
        this.port = port;
    }

    public static String generalName(String host, int port) {
        return NAME_PREFIX + host + "#" + port;
    }

    public static String folderName(String host, int port) {
        return host + "#" + port;
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
        this.rootCertFile = cfg.configPath().resolve(folderName(host, port)).resolve(CERT_FILE_NAME);
        if (!Files.exists(rootCertFile)) {
            throw new FileNotFoundException("CA cert file at location " + rootCertFile + " doesn't exists.");
        }
    }


    public InputStream openRootCertStream() throws IOException {
        return Files.newInputStream(rootCertFile);
    }
}
