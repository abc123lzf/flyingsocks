package com.lzf.flyingsocks.client.proxy;

/**
 * 代理服务器连接状态
 */
public enum ConnectionState {

    ;

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
