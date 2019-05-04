package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;

public class ProxyConfig extends AbstractConfig {

    private Method proxyMethod = Method.PAC;

    public ProxyConfig(ConfigManager<?> configManager, String name) {
        super(configManager, name);
    }

    private enum Method {
        DIRECT, PAC, GLOBAL;
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {

    }
}
