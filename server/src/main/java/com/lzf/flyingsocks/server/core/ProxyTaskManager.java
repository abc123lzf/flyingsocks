package com.lzf.flyingsocks.server.core;

interface ProxyTaskManager {

    /**
     * 注册代理任务订阅者
     * @param subscriber 代理任务订阅者
     */
    void registerSubscriber(ProxyTaskSubscriber subscriber);

    /**
     * 删除代理任务订阅者
     * @param subscriber 代理任务订阅者
     */
    void removeSubscriber(ProxyTaskSubscriber subscriber);

    /**
     * 发布代理任务
     * @param task 代理任务
     */
    void publish(ProxyTask task);
}
