package com.lzf.flyingsocks.client.gui.model;

import com.lzf.flyingsocks.Named;
import com.lzf.flyingsocks.client.proxy.server.ProxyServerConfig;

/**
 * @author lizifan
 * @since 2022.1.2 17:33
 */
public enum ServerEncryptType implements Named {

    NONE("不加密", ProxyServerConfig.EncryptType.NONE),
    TLS_12("TLS v1.2", ProxyServerConfig.EncryptType.SSL),
    TLS_12_CA("TLS v1.2 (CA证书)", ProxyServerConfig.EncryptType.SSL_CA);

    private final String name;

    private final ProxyServerConfig.EncryptType configEncryptType;

    ServerEncryptType(String name, ProxyServerConfig.EncryptType configEncryptType) {
        this.name = name;
        this.configEncryptType = configEncryptType;
    }

    public ProxyServerConfig.EncryptType getConfigEncryptType() {
        return configEncryptType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

}
