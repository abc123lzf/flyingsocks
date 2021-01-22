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
package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.ConfigEventListener;
import com.lzf.flyingsocks.client.proxy.ConnectionStateListener;
import com.lzf.flyingsocks.client.proxy.socks.SocksConfig;

import java.util.Map;

import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.Node;

/**
 * GUI操作接口
 *
 * @since 2019.8.18 1:17
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
     * 打开配置文件目录
     */
    void openConfigDirectory();

    /**
     * 打开浏览器
     *
     * @param url URL链接
     */
    void openBrowser(String url);

    /**
     * 注册配置中心事件监听器
     *
     * @param listener 事件监听器
     */
    void registerConfigEventListener(ConfigEventListener listener);

    /**
     * 注册ProxyServerConfig监听器
     *
     * @param event    关心的事件
     * @param runnable 事件触发后的逻辑
     * @param remove   事件触发后是否删除该监听器
     */
    void registerProxyServerConfigListener(String event, Runnable runnable, boolean remove);

    /**
     * 注册SocksConfig事件监听器
     *
     * @param event    关心的事件
     * @param runnable 事件触发后的逻辑
     * @param remove   事件触发后是否删除该监听器
     */
    void registerSocksConfigListener(String event, Runnable runnable, boolean remove);


    /**
     * 删除配置中心事件监听器
     *
     * @param listener 事件监听器
     */
    void removeConfigEventListener(ConfigEventListener listener);

    /**
     * 用户界面添加代理服务器配置
     *
     * @param node 服务器配置
     */
    void addServerConfig(Node node);

    /**
     * 更新代理服务器的配置
     *
     * @param node 服务器配置
     */
    void updateServerConfig(Node node);


    /**
     * 移除代理服务器配置
     *
     * @param node 服务器配置
     */
    void removeServer(Node node);

    /**
     * @return 服务器配置
     */
    Node[] getServerNodes();

    /**
     * @return 获取本地Socks代理包装对象
     */
    SocksConfig getSocksConfig();

    /**
     * @return 系统代理模式
     */
    int proxyMode();

    /**
     * @param mode 系统代理模式
     */
    void setProxyMode(int mode);

    /**
     * 启用代理服务器
     *
     * @param node 代理服务器配置
     * @param use  是否启用
     */
    void setProxyServerUsing(Node node, boolean use);

    /**
     * 批量修改代理服务器状态
     *
     * @param map 代理服务器与是否启用映射关系
     */
    void setProxyServerUsing(Map<Node, Boolean> map);


    /**
     * 修改本地Socks5代理端口身份验证机制
     *
     * @param port     本地Socks代理端口
     * @param auth     是否打开身份验证
     * @param username 用户名
     * @param password 密码
     */
    void updateSocksProxyAuthentication(int port, boolean auth, String username, String password);

    /**
     * 注册代理服务器连接状态监听器
     *
     * @param listener 监听器对象
     */
    void registerConnectionStateListener(Node node, ConnectionStateListener listener);

    /**
     * 查询代理服务器上传带宽
     *
     * @param node 代理服务器配置节点
     * @return 上传带宽，单位字节每秒
     */
    long queryProxyServerUploadThroughput(Node node);


    /**
     * 查询代理服务器下载带宽
     *
     * @param node 代理服务器配置节点
     * @return 下载带宽，单位字节每秒
     */
    long queryProxyServerDownloadThroughput(Node node);
}
