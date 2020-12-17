package com.lzf.flyingsocks.client.proxy;


import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.lzf.flyingsocks.client.proxy.ProxyRequest.Protocol;

/**
 * 代理请求消息订阅者
 */
public interface ProxyRequestSubscriber {

    Set<Protocol> ANY_PROTOCOL = Collections.unmodifiableSet(EnumSet.allOf(Protocol.class));

    Set<Protocol> ONLY_TCP = Collections.unmodifiableSet(Collections.singleton(Protocol.TCP));

    Set<Protocol> ONLY_UDP = Collections.unmodifiableSet(Collections.singleton(Protocol.UDP));

    /**
     * 接收消息
     * @param request ProxyRequest请求
     */
    void receive(ProxyRequest request);

    /**
     * @return 是否接收需要代理的消息(即经过PAC判定需要代理的消息)
     */
    default boolean receiveNeedProxy() {
        return false;
    }

    /**
     * @return 是否接收无需代理的消息(即经过PAC判定无需代理的消息)
     */
    default boolean receiveNeedlessProxy() {
        return false;
    }

    /**
     * @return 接收的代理消息类型
     */
    default Class<? extends ProxyRequest> requestType() {
        return ProxyRequest.class;
    }

    /**
     * @return 可以接收的代理协议Set
     */
    default Set<Protocol> requestProtocol() {
        return ANY_PROTOCOL;
    }
}