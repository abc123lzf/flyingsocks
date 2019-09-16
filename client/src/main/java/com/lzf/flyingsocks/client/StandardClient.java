package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.gui.swt.SWTViewComponent;
import com.lzf.flyingsocks.client.proxy.ProxyAutoConfig;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;
import com.lzf.flyingsocks.client.proxy.socks.SocksConfig;
import com.lzf.flyingsocks.client.proxy.socks.SocksProxyComponent;
import com.lzf.flyingsocks.client.gui.swing.SwingViewComponent;

import java.util.Map;

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
        GlobalConfig cfg = new GlobalConfig(getConfigManager());
        getConfigManager().registerConfig(cfg);

        addComponent(new SocksProxyComponent(this));
        if(!isWindows()) {
            addComponent(new SwingViewComponent(this));
        } else {
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
    public void updateSocksProxyAuthentication(boolean auth, String username, String password) {
        SocksConfig cfg = getConfigManager().getConfig(SocksConfig.NAME, SocksConfig.class);
        if(cfg == null)
            return;
        cfg.update(auth, username, password);
    }

    @Override
    public void registerConfigEventListener(ConfigEventListener listener) {
        getConfigManager().registerConfigEventListener(listener);
    }

    @Override
    public void registerProxyServerConfigListener(String event, Runnable runnable, boolean remove) {
        if(!remove) {
            getConfigManager().registerConfigEventListener(e -> {
                if(e.getSource() instanceof ProxyServerConfig && e.getEvent().equals(event)) {
                    runnable.run();
                }
            });
        } else {
            getConfigManager().registerConfigEventListener(new ConfigEventListener() {
                @Override
                public void configEvent(ConfigEvent e) {
                    if(e.getSource() instanceof ProxyServerConfig && e.getEvent().equals(event)) {
                        runnable.run();
                        e.getConfigManager().removeConfigEventListener(this);
                    }
                }
            });
        }
    }

    @Override
    public void registerSocksConfigListener(String event, Runnable runnable, boolean remove) {
        if(!remove) {
            getConfigManager().registerConfigEventListener(e -> {
                if(e.getSource() instanceof SocksConfig && e.getEvent().equals(event)) {
                    runnable.run();
                }
            });
        } else {
            getConfigManager().registerConfigEventListener(new ConfigEventListener() {
                @Override
                public void configEvent(ConfigEvent e) {
                    if(e.getSource() instanceof SocksConfig && e.getEvent().equals(event)) {
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
        if(cfg != null)
            cfg.addProxyServerNode(node);
    }

    @Override
    public void updateServerConfig(Node node) {
        ProxyServerConfig cfg = getProxyServerConfig();
        if(cfg != null)
            cfg.updateProxyServerNode(node);
    }

    @Override
    public void removeServer(Node node) {
        ProxyServerConfig cfg = getProxyServerConfig();
        if(cfg != null)
            cfg.removeProxyServerNode(node);
    }

    @Override
    public Node[] getServerNodes() {
        ProxyServerConfig cfg = getProxyServerConfig();
        if(cfg == null)
            return null;
        return cfg.getProxyServerConfig();
    }

    @Override
    public int proxyMode() {
        ProxyAutoConfig cfg = getConfigManager().getConfig(ProxyAutoConfig.DEFAULT_NAME, ProxyAutoConfig.class);
        if(cfg == null)
            return -1;

        return cfg.getProxyMode();
    }

    @Override
    public void setProxyMode(int mode) {
        ProxyAutoConfig cfg = getProxyAutoConfig();
        if(cfg == null)
            return;
        cfg.setProxyMode(mode);
    }

    @Override
    public void setProxyServerUsing(Node node, boolean use) {
        ProxyServerConfig cfg = getProxyServerConfig();
        if(cfg == null)
            return;
        cfg.setProxyServerUsing(node, use);
    }

    @Override
    public void setProxyServerUsing(Map<Node, Boolean> map) {
        ProxyServerConfig cfg = getProxyServerConfig();
        if(cfg == null)
            return;

        cfg.setProxyServerUsing(map);
    }

    private ProxyServerConfig getProxyServerConfig() {
        return getConfigManager().getConfig(ProxyServerConfig.DEFAULT_NAME, ProxyServerConfig.class);
    }

    private ProxyAutoConfig getProxyAutoConfig() {
        return getConfigManager().getConfig(ProxyAutoConfig.DEFAULT_NAME, ProxyAutoConfig.class);
    }
}
