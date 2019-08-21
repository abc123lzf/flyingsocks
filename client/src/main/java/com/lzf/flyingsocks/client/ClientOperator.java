package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.ConfigEventListener;
import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.Node;

/**
 * @create 2019.8.18 1:17
 * @description GUI操作接口
 */
public interface ClientOperator {

    /**
     * 清空日志
     */
    void cleanLogFiles();

    /**
     * 打开日志目录
     */
    void openLogDirectory();

    /**
     * 打开浏览器
     * @param url URL链接
     */
    void openBrowser(String url);

    /**
     * 注册配置中心事件监听器
     * @param listener 事件监听器
     */
    void registerConfigEventListener(ConfigEventListener listener);

    /**
     * 注册ProxyServerConfig监听器
     * @param event 关心的事件
     * @param runnable 事件触发后的逻辑
     * @param remove 事件触发后是否删除该监听器
     */
    void registerProxyServerConfigListener(String event, Runnable runnable, boolean remove);

    /**
     * 删除配置中心事件监听器
     * @param listener 事件监听器
     */
    void removeConfigEventListener(ConfigEventListener listener);

    /**
     * 用户界面添加代理服务器配置
     * @param node 服务器配置
     */
    void addServerConfig(Node node);

    /**
     * 更新代理服务器的配置
     * @param node 服务器配置
     */
    void updateServerConfig(Node node);


    /**
     * 移除代理服务器配置
     * @param node 服务器配置
     */
    void removeServer(Node node);

    /**
     * @return 服务器配置
     */
    Node[] getServerNodes();

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
    void setProxyServerUsing(Node node, boolean use);

    /**
     * 修改本地Socks5代理端口身份验证机制
     * @param auth 是否打开身份验证
     * @param username 用户名
     * @param password 密码
     */
    void updateSocksProxyAuthentication(boolean auth, String username, String password);
}
