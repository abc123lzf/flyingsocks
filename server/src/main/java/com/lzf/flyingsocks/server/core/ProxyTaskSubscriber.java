package com.lzf.flyingsocks.server.core;

/**
 * 代理任务订阅者
 * @see com.lzf.flyingsocks.server.core.ProxyTask
 */
interface ProxyTaskSubscriber {

    /**
     * 主动接收代理任务
     * @param task 代理任务对象
     */
    void receive(ProxyTask task);

}
