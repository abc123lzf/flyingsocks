package com.lzf.flyingsocks.client.gui.model;

import com.lzf.flyingsocks.Named;
import com.lzf.flyingsocks.client.proxy.server.ProxyServerConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author lizifan 695199262@qq.com
 * @since 2021.9.25 21:14
 */
public class ServerVO implements Named {

    private String hostName;

    private Integer port;

    private Integer certPort;

    private ServerAuthType authType;

    private ServerEncryptType encryptType;

    private String userName;

    private String password;

    private ProxyServerConfig.Node configuration;

    private String aliasName;

    public ServerVO() {
        this.aliasName = "New Config";
    }

    public ServerVO(ProxyServerConfig.Node configuration) {
        setConfiguration(Objects.requireNonNull(configuration));
    }

    public boolean isNew() {
        return this.configuration == null;
    }

    public ProxyServerConfig.Node getConfiguration() {
        return configuration;
    }

    public ProxyServerConfig.Node buildServerConfig() {
        var config = this.configuration;
        if (config == null) {
            config = new ProxyServerConfig.Node();
        }

        config.setHost(hostName);
        config.setPort(port);
        config.setCertPort(certPort);
        config.setAuthType(authType.getConfigAuthType());
        config.setEncryptType(encryptType.getConfigEncryptType());

        if (authType == ServerAuthType.COMMON) {
            config.setAuthArgument(Collections.singletonMap("password", password));
        } else if (authType == ServerAuthType.USER) {
            Map<String, String> authArgs = new HashMap<>();
            authArgs.put("user", userName);
            authArgs.put("pass", password);
            config.setAuthArgument(authArgs);
        }

        return config;
    }


    public void setConfiguration(ProxyServerConfig.Node configuration) {
        this.configuration = configuration;
        if (configuration != null) {
            this.hostName = configuration.getHost();
            this.port = configuration.getPort();
            switch (configuration.getAuthType()) {
                case SIMPLE: {
                    this.authType = ServerAuthType.COMMON;
                    this.password = configuration.getAuthArgument("password");
                } break;
                case USER: {
                    this.authType = ServerAuthType.USER;
                    this.userName = configuration.getAuthArgument("user");
                    this.password = configuration.getAuthArgument("pass");
                } break;
                default:
                    throw new IllegalArgumentException();
            }

            switch (configuration.getEncryptType()) {
                case NONE: this.encryptType = ServerEncryptType.NONE; break;
                case SSL: this.encryptType = ServerEncryptType.TLS_12; break;
                case SSL_CA: this.encryptType = ServerEncryptType.TLS_12_CA; break;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getCertPort() {
        return certPort;
    }

    public void setCertPort(Integer certPort) {
        this.certPort = certPort;
    }

    public ServerAuthType getAuthType() {
        return authType;
    }

    public void setAuthType(ServerAuthType authType) {
        this.authType = authType;
    }

    public ServerEncryptType getEncryptType() {
        return encryptType;
    }

    public void setEncryptType(ServerEncryptType encryptType) {
        this.encryptType = encryptType;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getName() {
        if (hostName == null || hostName.isBlank() || port == null) {
            return aliasName;
        }
        return hostName + ":" + port;
    }

    @Override
    public String toString() {
        return getName();
    }
}
