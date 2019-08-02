package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.Component;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.Environment;
import com.lzf.flyingsocks.VoidComponent;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;

public interface Client extends Component<VoidComponent>, Environment {

    /**
     * 默认组件名
     */
    String DEFAULT_COMPONENT_NAME = "flyingsocks-client";

    /**
     * 当前版本号
     */
    String VERSION = "v1.1";

    /**
     * @return 配置管理器
     */
    ConfigManager<?> getConfigManager();


    /**
     * 用户界面添加代理服务器配置
     * @param node 服务器配置
     */
    void addServerConfig(ProxyServerConfig.Node node);

    /**
     * 更新代理服务器的配置
     * @param node 服务器配置
     */
    void updateServerConfig(ProxyServerConfig.Node node);


    /**
     * 移除代理服务器配置
     * @param node 服务器配置
     */
    void removeServer(ProxyServerConfig.Node node);

    /**
     * @return 服务器配置
     */
    ProxyServerConfig.Node[] getServerNodes();
}
