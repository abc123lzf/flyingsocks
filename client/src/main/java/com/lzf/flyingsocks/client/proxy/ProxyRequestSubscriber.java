package com.lzf.flyingsocks.client.proxy;

/**
 * 代理请求消息订阅者
 */
public interface ProxyRequestSubscriber {

    /**
     * 接收消息
     * @param request ProxyRequest请求
     */
    void receive(ProxyRequest request);

    /**
     * @return 是否接收需要代理的消息(即经过PAC判定需要代理的消息)
     */
    boolean receiveNeedProxy();

    /**
     * @return 是否接收无需代理的消息(即经过PAC判定无需代理的消息)
     */
    boolean receiveNeedlessProxy();

    /**
     * @return 接收的代理消息类型
     */
    Class<? extends ProxyRequest> requestType();
}
