package com.lzf.flyingsocks.client.gui.swing.model;

import com.lzf.flyingsocks.Named;
import com.lzf.flyingsocks.client.proxy.server.ProxyServerConfig;

/**
 * @author lizifan 695199262@qq.com
 * @since 2021.9.25 21:14
 */
public class ServerVO implements Named {

    private final ProxyServerConfig.Node configuration;

    private String aliasName;

    public ServerVO(ProxyServerConfig.Node configuration) {
        this.configuration = configuration;
    }

    public boolean isSameConfiguration(ProxyServerConfig.Node configuration) {
        return this.configuration == configuration;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public String getHostname() {
        return configuration.getHost();
    }

    public int getPort() {
        return configuration.getPort();
    }

    public ServerEncryptType getServerEncryptType() {
        ProxyServerConfig.EncryptType encryptType = configuration.getEncryptType();
        switch (encryptType) {
            case NONE: return ServerEncryptType.NONE;
            case SSL: return ServerEncryptType.TLS_12;
            case SSL_CA: return ServerEncryptType.TLS_12_CA;
        }
        throw new IllegalStateException();
    }

    public Integer getCertPort() {
        return configuration.getCertPort();
    }

    public ServerAuthType getServerAuthType() {
        ProxyServerConfig.AuthType authType = configuration.getAuthType();
        switch (authType) {
            case SIMPLE: return ServerAuthType.COMMON;
            case USER: return ServerAuthType.USER;
        }
        throw new IllegalStateException();
    }

    public String getUserName() {
        return configuration.getAuthArgument("user");
    }

    public String getPassword() {
        return configuration.getAuthArgument("pass");
    }

    public enum ServerEncryptType implements Named {
        NONE("不加密"),
        TLS_12("TLS v1.2"),
        TLS_12_CA("TLS v1.2 (CA证书)");

        private final String name;

        ServerEncryptType(String name) {
            this.name = name;
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


    public enum ServerAuthType implements Named {
        COMMON("普通认证"),
        USER("用户认证");

        private final String name;

        ServerAuthType(String name) {
            this.name = name;
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


    @Override
    public String getName() {
        return aliasName != null ? aliasName : (configuration.getHost() + ":" + configuration.getPort());
    }

    @Override
    public String toString() {
        return getName();
    }
}
