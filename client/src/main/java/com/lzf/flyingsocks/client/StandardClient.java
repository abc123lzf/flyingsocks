package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.proxy.socks.SocksProxyComponent;
import com.lzf.flyingsocks.client.view.ViewComponent;

public class StandardClient extends TopLevelComponent implements Client {

    StandardClient() {
        super(DEFAULT_COMPONENT_NAME);
    }

    @Override
    protected void initInternal() {
        GlobalConfig cfg = new GlobalConfig(getConfigManager());
        getConfigManager().registerConfig(cfg);

        addComponent(new SocksProxyComponent(this));
        addComponent(new ViewComponent(this));
        super.initInternal();
    }

    @Override
    protected void startInternal() {
        super.startInternal();
    }

    @Override
    protected void stopInternal() {
        super.stopInternal();
        getConfigManager().saveAllConfig();
        System.exit(0);
    }

    @Override
    protected void restartInternal() {
        throw new ComponentException("can not restart client");
    }

    @Override
    public ConfigManager<?> getConfigManager() {
        return super.getConfigManager();
    }

    @Override
    public String getSystemProperties(String key) {
        return System.getProperty(key);
    }

    @Override
    public String getEnvironmentVariable(String key) {
        return System.getenv(key);
    }

    @Override
    public void setSystemProperties(String key, String value) {
        System.setProperty(key, value);
    }


}
