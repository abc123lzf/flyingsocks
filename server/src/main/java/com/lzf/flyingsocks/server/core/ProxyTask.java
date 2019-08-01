package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.protocol.ProxyRequestMessage;

import java.util.Objects;

/**
 * 代理任务对象
 */
class ProxyTask implements Cloneable {

    private final ProxyRequestMessage proxyRequestMessage;

    private final ClientSession session;

    ProxyTask(ProxyRequestMessage proxyRequestMessage, ClientSession clientSession) {
        this.proxyRequestMessage = Objects.requireNonNull(proxyRequestMessage, "ProxyRequestMessage must not be null");
        this.session = Objects.requireNonNull(clientSession, "ClientSession must not be null");
    }

    ProxyRequestMessage getProxyRequestMessage() {
        return proxyRequestMessage;
    }

    ClientSession getSession() {
        return session;
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public ProxyTask clone() throws CloneNotSupportedException {
        return new ProxyTask(proxyRequestMessage.clone(), session);
    }

    @Override
    public String toString() {
        return "ProxyTask{" +
                "proxyRequestMessage=" + proxyRequestMessage +
                ", session=" + session +
                '}';
    }
}
