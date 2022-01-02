package com.lzf.flyingsocks.client.gui.model;

import com.lzf.flyingsocks.Named;
import com.lzf.flyingsocks.client.proxy.server.ProxyServerConfig;

/**
 * @author lizifan
 * @since 2022.1.2 17:31
 */
public enum ServerAuthType implements Named {

    COMMON("普通认证", ProxyServerConfig.AuthType.SIMPLE),

    USER("用户认证", ProxyServerConfig.AuthType.USER);

    private final String name;

    private final ProxyServerConfig.AuthType configAuthType;

    ServerAuthType(String name, ProxyServerConfig.AuthType configAuthType) {
        this.name = name;
        this.configAuthType = configAuthType;
    }

    public ProxyServerConfig.AuthType getConfigAuthType() {
        return configAuthType;
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
