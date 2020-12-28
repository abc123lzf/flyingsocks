package com.lzf.flyingsocks.client.proxy.http;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;

/**
 * @author lzf abc123lzf@126.com
 * @since 2020/12/26 3:03
 */
public class HttpProxyConfig extends AbstractConfig {

    public static final String NAME = "config.httpproxy";

    private String username;

    private String password;

    public HttpProxyConfig(ConfigManager<?> configManager) {
        super(configManager, NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {

    }
}
