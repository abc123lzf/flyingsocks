package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;

public abstract class Client extends TopLevelComponent
        implements Component<VoidComponent>, Environment, ClientOperator {

    /**
     * 默认组件名
     */
    public static final String DEFAULT_COMPONENT_NAME = "flyingsocks-client";

    /**
     * 当前版本号
     */
    public static final String VERSION = "v1.1";

    public Client() {
        super(DEFAULT_COMPONENT_NAME);
    }

    /**
     * @return 配置管理器
     */
    public final ConfigManager<?> getConfigManager() {
        return super.getConfigManager();
    }


    /* GUI Interface  */

    /**
     * 用户界面添加代理服务器配置
     * @param node 服务器配置
     */
    public abstract void addServerConfig(ProxyServerConfig.Node node);

    /**
     * 更新代理服务器的配置
     * @param node 服务器配置
     */
    public abstract void updateServerConfig(ProxyServerConfig.Node node);


    /**
     * 移除代理服务器配置
     * @param node 服务器配置
     */
    public abstract void removeServer(ProxyServerConfig.Node node);

    /**
     * @return 服务器配置
     */
    public abstract ProxyServerConfig.Node[] getServerNodes();

    /**
     * @return 系统代理模式
     */
    public abstract int proxyMode();
}
