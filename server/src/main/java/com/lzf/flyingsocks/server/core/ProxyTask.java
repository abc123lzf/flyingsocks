package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.protocol.ProxyRequestMessage;

class ProxyTask {

    private final ProxyRequestMessage proxyRequestMessage;

    private final ClientSession session;

    ProxyTask(ProxyRequestMessage proxyRequestMessage, ClientSession clientSession) {
        this.proxyRequestMessage = proxyRequestMessage;
        this.session = clientSession;
    }

    ProxyRequestMessage getProxyRequestMessage() {
        return proxyRequestMessage;
    }

    ClientSession getSession() {
        return session;
    }

    @Override
    public String toString() {
        return "ProxyTask{" +
                "proxyRequestMessage=" + proxyRequestMessage +
                ", session=" + session +
                '}';
    }
}
