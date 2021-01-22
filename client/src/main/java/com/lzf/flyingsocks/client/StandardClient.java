/*
 * Copyright (c) 2019 abc123lzf <abc123lzf@126.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.ConfigEvent;
import com.lzf.flyingsocks.ConfigEventListener;
import com.lzf.flyingsocks.client.gui.swt.SWTViewComponent;
import com.lzf.flyingsocks.client.proxy.ConnectionStateListener;
import com.lzf.flyingsocks.client.proxy.ProxyAutoConfig;
import com.lzf.flyingsocks.client.proxy.ProxyComponent;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;
import com.lzf.flyingsocks.client.proxy.socks.SocksConfig;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.Node;

/**
 * 客户端类
 */
public final class StandardClient extends Client {

    StandardClient() {
        super();
    }

    /**
     * 加载基本配置
     */
    @Override
    protected void initInternal() {
        try {
            Properties p = new Properties();
            p.load(loadResource("classpath://config.properties"));
            p.forEach((k, v) -> setSystemProperties((String) k, (String) v));
        } catch (IOException e) {
            log.error("Read config.properties occur a exception", e);
            System.exit(1);
        }

        GlobalConfig cfg = new GlobalConfig(getConfigManager());
        getConfigManager().registerConfig(cfg);

        addComponent(new ProxyComponent(this));

        if (cfg.isOpenGUI()) {
            addComponent(new SWTViewComponent(this));
        }

        super.initInternal();
    }

    /**
     * 直接启动子组件
     */
    @Override
    protected void startInternal() {
        super.startInternal();
    }

    /**
     * 点击GUI界面退出按钮时调用
     * 首先暂停所有组件，然后保存所有配置
     */
    @Override
    protected void stopInternal() {
        super.stopInternal();
        getConfigManager().saveAllConfig();
        System.exit(0);
    }

    /**
     * 暂不支持整个客户端的重启
     */
    @Override
    protected void restartInternal() {
        throw new ComponentException("can not restart client");
    }

    @Override
    public void updateSocksProxyAuthentication(int port, boolean auth, String username, String password) {
        SocksConfig cfg = getConfigManager().getConfig(SocksConfig.NAME, SocksConfig.class);
        if (cfg == null) {
            return;
        }

        cfg.update(port, auth, username, password);
    }

    @Override
    public void registerConfigEventListener(ConfigEventListener listener) {
        getConfigManager().registerConfigEventListener(listener);
    }

    @Override
    public void registerProxyServerConfigListener(String event, Runnable runnable, boolean remove) {
        if (!remove) {
            getConfigManager().registerConfigEventListener(e -> {
                if (e.getSource() instanceof ProxyServerConfig && e.getEvent().equals(event)) {
                    runnable.run();
                }
            });
        } else {
            getConfigManager().registerConfigEventListener(new ConfigEventListener() {
                @Override
                public void configEvent(ConfigEvent e) {
                    if (e.getSource() instanceof ProxyServerConfig && e.getEvent().equals(event)) {
                        runnable.run();
                        e.getConfigManager().removeConfigEventListener(this);
                    }
                }
            });
        }
    }

    @Override
    public void registerSocksConfigListener(String event, Runnable runnable, boolean remove) {
        if (!remove) {
            getConfigManager().registerConfigEventListener(e -> {
                if (e.getSource() instanceof SocksConfig && e.getEvent().equals(event)) {
                    runnable.run();
                }
            });
        } else {
            getConfigManager().registerConfigEventListener(new ConfigEventListener() {
                @Override
                public void configEvent(ConfigEvent e) {
                    if (e.getSource() instanceof SocksConfig && e.getEvent().equals(event)) {
                        runnable.run();
                        e.getConfigManager().removeConfigEventListener(this);
                    }
                }
            });
        }
    }

    @Override
    public void removeConfigEventListener(ConfigEventListener listener) {
        getConfigManager().removeConfigEventListener(listener);
    }

    @Override
    public void addServerConfig(Node node) {
        ProxyServerConfig cfg = getProxyServerConfig();
        if (cfg != null) {
            cfg.addProxyServerNode(node);
        }
    }

    @Override
    public void updateServerConfig(Node node) {
        ProxyServerConfig cfg = getProxyServerConfig();
        if (cfg != null) {
            cfg.updateProxyServerNode(node);
        }

    }

    @Override
    public void removeServer(Node node) {
        ProxyServerConfig cfg = getProxyServerConfig();
        if (cfg != null) {
            cfg.removeProxyServerNode(node);
        }
    }

    @Override
    public Node[] getServerNodes() {
        ProxyServerConfig cfg = getProxyServerConfig();
        if (cfg == null) {
            return null;
        }
        return cfg.getProxyServerConfig();
    }

    @Override
    public int proxyMode() {
        ProxyAutoConfig cfg = getConfigManager().getConfig(ProxyAutoConfig.DEFAULT_NAME, ProxyAutoConfig.class);
        if (cfg == null) {
            return -1;
        }

        return cfg.getProxyMode();
    }

    @Override
    public void setProxyMode(int mode) {
        ProxyAutoConfig cfg = getProxyAutoConfig();
        if (cfg == null) {
            return;
        }

        cfg.setProxyMode(mode);
    }

    @Override
    public void setProxyServerUsing(Node node, boolean use) {
        ProxyServerConfig cfg = getProxyServerConfig();
        if (cfg == null) {
            return;
        }
        cfg.setProxyServerUsing(node, use);
    }

    @Override
    public void setProxyServerUsing(Map<Node, Boolean> map) {
        ProxyServerConfig cfg = getProxyServerConfig();
        if (cfg == null) {
            return;
        }

        cfg.setProxyServerUsing(map);
    }


    @Override
    public void registerConnectionStateListener(Node node, ConnectionStateListener listener) {
        ProxyComponent pc = getComponentByName(ProxyComponent.NAME, ProxyComponent.class);
        pc.registerProxyServerConnectionStateListener(node.getHost(), node.getPort(), listener);
    }

    @Override
    public SocksConfig getSocksConfig() {
        SocksConfig cfg = getConfigManager().getConfig(SocksConfig.NAME, SocksConfig.class);
        return cfg.configFacade();
    }

    @Override
    public long queryProxyServerUploadThroughput(Node node) {
        ProxyComponent pc = getComponentByName(ProxyComponent.NAME, ProxyComponent.class);
        return pc.queryProxyServerUploadThroughput(node.getHost(), node.getPort());
    }

    @Override
    public long queryProxyServerDownloadThroughput(Node node) {
        ProxyComponent pc = getComponentByName(ProxyComponent.NAME, ProxyComponent.class);
        return pc.queryProxyServerDownloadThroughput(node.getHost(), node.getPort());
    }

    private ProxyServerConfig getProxyServerConfig() {
        return getConfigManager().getConfig(ProxyServerConfig.DEFAULT_NAME, ProxyServerConfig.class);
    }

    private ProxyAutoConfig getProxyAutoConfig() {
        return getConfigManager().getConfig(ProxyAutoConfig.DEFAULT_NAME, ProxyAutoConfig.class);
    }
}
