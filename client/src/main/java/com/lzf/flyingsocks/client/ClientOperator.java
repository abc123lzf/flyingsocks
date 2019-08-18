package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.ConfigEventListener;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;

/**
 * @create 2019.8.18 1:17
 * @description GUI操作接口
 */
public interface ClientOperator {

    /**
     * 注册配置中心事件监听器
     * @param listener 事件监听器
     */
    void registerConfigEventListener(ConfigEventListener listener);

    /**
     * 删除配置中心事件监听器
     * @param listener 事件监听器
     */
    void removeConfigEventListener(ConfigEventListener listener);

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

    /**
     * @return 系统代理模式
     */
    int proxyMode();

    /**
     *
     * @param mode 系统代理模式
     */
    void setProxyMode(int mode);

    /**
     * 启用代理服务器
     * @param node 代理服务器配置
     * @param use 是否启用
     */
    void setProxyServerUsing(ProxyServerConfig.Node node, boolean use);
}
