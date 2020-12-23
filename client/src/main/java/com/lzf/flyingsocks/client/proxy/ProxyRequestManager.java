package com.lzf.flyingsocks.client.proxy;

public interface ProxyRequestManager {

    /**
     * 注册代理请求订阅者
     *
     * @param subscriber ProxyRequestSubscriber对象
     */
    void registerSubscriber(ProxyRequestSubscriber subscriber);

    /**
     * 删除代理请求订阅者
     *
     * @param subscriber ProxyRequestSubscriber对象
     */
    void removeSubscriber(ProxyRequestSubscriber subscriber);

    /**
     * 发布代理请求
     *
     * @param request 代理请求
     */
    void publish(ProxyRequest request);
}
