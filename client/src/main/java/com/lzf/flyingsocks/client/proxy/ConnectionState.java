package com.lzf.flyingsocks.client.proxy;

/**
 * 代理服务器连接状态
 */
public enum ConnectionState {

    NEW(true, false),
    SSL_INITIAL(true, false),
    SSL_CONNECTING(true, false),
    SSL_CONNECT_TIMEOUT(false, true),
    SSL_CONNECT(true, false),
    SSL_CONNECT_AUTH_FAILURE(false, false),
    SSL_CONNECT_ERROR(false, false),
    SSL_CONNECT_DONE(true, false),
    PROXY_INITIAL(true, false),
    PROXY_CONNECTING(true, false),
    PROXY_CONNECT_TIMEOUT(false, true),
    PROXY_CONNECT(true, false),
    PROXY_DISCONNECT(false, true),
    PROXY_CONNECT_AUTH_FAILURE(false, false),
    PROXY_CONNECT_ERROR(false, false),
    UNUSED(true, false);


    /**
     * 当前状态是否是正常的
     */
    private final boolean normal;

    /**
     * 是否可重试连接
     */
    private final boolean retrievable;


    public boolean isNormal() {
        return normal;
    }

    public boolean canRetry() {
        return retrievable;
    }

    ConnectionState(boolean normal, boolean retrievable) {
        this.retrievable = retrievable;
        this.normal = normal;
    }
}
