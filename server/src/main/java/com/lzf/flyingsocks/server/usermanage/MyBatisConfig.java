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
package com.lzf.flyingsocks.server.usermanage;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/2/15 12:59 下午
 */
public class MyBatisConfig extends AbstractConfig {

    public static final String NAME = "MyBatisConfig";

    private static final String DEFAULT_ENV_ID = "product";
    private static final String ENV_ID_PROPERTIES_KEY = "flyingsocks.usermanage.mybatis.envid";
    private static final String CONFIG_URL_PROPERTIES_KEY = "flyingsocks.usermanage.mybatis.config";
    private static final String DEFAULT_CONFIG_URL = "classpath://mybatis-config.xml";

    private String environmentId;

    private URL configUrl;

    MyBatisConfig(ConfigManager<?> configManager) {
        super(Objects.requireNonNull(configManager), NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        String envId = configManager.getSystemProperties(ENV_ID_PROPERTIES_KEY);
        if (StringUtils.isBlank(envId)) {
            envId = DEFAULT_ENV_ID;
        }

        this.environmentId = envId;

        String configUrl = configManager.getSystemProperties(CONFIG_URL_PROPERTIES_KEY);
        if (StringUtils.isBlank(configUrl)) {
            configUrl = DEFAULT_CONFIG_URL;
        }

        try {
            this.configUrl = new URL(configUrl);
        } catch (MalformedURLException e) {
            throw new ConfigInitializationException(e);
        }
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public InputStream openConfigInputStream() throws IOException {
        return configUrl.openStream();
    }
}
