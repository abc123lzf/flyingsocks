package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;

public class SocksConfig extends AbstractConfig {

    private boolean needAuth = false;

    public SocksConfig(ConfigManager<?> configManager, String name) {
        super(configManager, name);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {

    }
}
