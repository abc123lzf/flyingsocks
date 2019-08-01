package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;
import com.lzf.flyingsocks.client.proxy.socks.SocksProxyComponent;
import com.lzf.flyingsocks.client.view.ViewComponent;

/**
 * 客户端类
 */
public class StandardClient extends TopLevelComponent implements Client {

    StandardClient() {
        super(DEFAULT_COMPONENT_NAME);
    }

    /**
     * 加载基本配置
     */
    @Override
    protected void initInternal() {
        GlobalConfig cfg = new GlobalConfig(getConfigManager());
        getConfigManager().registerConfig(cfg);

        addComponent(new SocksProxyComponent(this));
        addComponent(new ViewComponent(this));
        super.initInternal();
    }

    /**
     * 直接启动子组件
     */
    @Override
    protected void startInternal() {
        super.startInternal();
    }

    /**
     * 点击GUI界面退出按钮时调用
     * 首先暂停所有组件，然后保存所有配置
     */
    @Override
    protected void stopInternal() {
        super.stopInternal();
        getConfigManager().saveAllConfig();
        System.exit(0);
    }

    /**
     * 暂不支持整个客户端的重启
     */
    @Override
    protected void restartInternal() {
        throw new ComponentException("can not restart client");
    }

    @Override
    public ConfigManager<?> getConfigManager() {
        return super.getConfigManager();
    }


    @Override
    public void addServerConfig(ProxyServerConfig.Node node) {
        ProxyServerConfig cfg = getConfigManager().getConfig(ProxyServerConfig.DEFAULT_NAME, ProxyServerConfig.class);
        cfg.updateProxyServerNode(node);
    }


}
