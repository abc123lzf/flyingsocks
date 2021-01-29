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
package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class OpenSSLConfig extends AbstractConfig {
    /**
     * 默认配置名前缀
     */
    private static final String NAME_PREFIX = "config.OpenSSL";

    /**
     * 证书文件URL,如果是PEM格式直接重命名为CRT格式
     */
    private final URL rootCertURL;

    /**
     * PCKS8编码的私钥文件
     */
    private final URL privateKeyURL;

    public OpenSSLConfig(ConfigManager<?> configManager, String nodeName) {
        super(configManager, configName(nodeName));
        try {
            this.rootCertURL = new URL(String.format("classpath://encrypt/%s/ca.crt", nodeName));
            this.privateKeyURL = new URL(String.format("classpath://encrypt/%s/private.key", nodeName));
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

    public static String configName(String nodeName) {
        return NAME_PREFIX + "." + nodeName;
    }
}
