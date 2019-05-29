package com.lzf.flyingsocks.client.view;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.proxy.ProxyAutoConfig;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

class TrayModule extends AbstractModule<ViewComponent> {
    private static final Logger log = LoggerFactory.getLogger("PopupMenu");

    static final String NAME = "module.tray";

    private final TrayIcon tray;

    private final Image icon;

    private final ConfigManager<?> configManager;

    private PACSettingMenu pacSettingMenu;

    private ServerChooseMenu serverChooseMenu;

    TrayModule(ViewComponent component, Image icon) {
        super(component, NAME);
        this.configManager = getComponent().getParentComponent().getConfigManager();
        this.icon = icon;
        this.tray = initTray();
    }

    private final class PACSettingMenu {
        int select;
        ProxyAutoConfig pacConfig;
        final MenuItem proxyItemClose = new MenuItem("关闭代理");
        final MenuItem proxyItemPac = new MenuItem("PAC模式");
        final MenuItem proxyItemGlobal = new MenuItem("全局模式");

        private PACSettingMenu(PopupMenu menu) {
            Menu proxyItem = new Menu("代理模式");
            proxyItem.add(proxyItemPac);
            proxyItem.add(proxyItemGlobal);
            proxyItem.add(proxyItemClose);

            proxyItemClose.addActionListener(e -> {
                if(select == ProxyAutoConfig.PROXY_NO)
                    return;
                if(pacConfig == null)
                    return;
                select = ProxyAutoConfig.PROXY_NO;
                pacConfig.setProxyMode(select);
            });

            proxyItemPac.addActionListener(e -> {
                if(select == ProxyAutoConfig.PROXY_PAC)
                    return;
                if(pacConfig == null)
                    return;

                select = ProxyAutoConfig.PROXY_PAC;
                pacConfig.setProxyMode(select);
            });

            proxyItemGlobal.addActionListener(e -> {
                if(select == ProxyAutoConfig.PROXY_GLOBAL)
                    return;
                if(pacConfig == null)
                    return;

                select = ProxyAutoConfig.PROXY_GLOBAL;
                pacConfig.setProxyMode(select);
            });

            configManager.registerConfigEventListener(event -> {
                if(event.getEvent().equals(Config.UPDATE_EVENT) && event.getSource() instanceof ProxyAutoConfig) {
                    initProxyAutoConfig((ProxyAutoConfig) event.getSource());
                }
            });

            menu.add(proxyItem);
        }

        private void initProxyAutoConfig(ProxyAutoConfig cfg) {
            switch (this.select = cfg.getProxyMode()) {
                case ProxyAutoConfig.PROXY_NO:
                    proxyItemClose.setLabel("√ 关闭代理");
                    proxyItemPac.setLabel("PAC模式");
                    proxyItemGlobal.setLabel("全局模式");
                    break;
                case ProxyAutoConfig.PROXY_PAC:
                    proxyItemClose.setLabel("关闭代理");
                    proxyItemPac.setLabel("√ PAC模式");
                    proxyItemGlobal.setLabel("全局模式");
                    break;
                case ProxyAutoConfig.PROXY_GLOBAL:
                    proxyItemClose.setLabel("关闭代理");
                    proxyItemPac.setLabel("PAC模式");
                    proxyItemGlobal.setLabel("√ 全局模式");
                    break;
                default:
                    log.error("Unknown ProxyAuto mode.");
            }

            this.pacConfig = cfg;
        }
    }

    /**
     * 代理服务器选择列表
     */
    private final class ServerChooseMenu {
        int select = -1;
        final Menu serverMenu = new Menu("选择服务器");
        final Map<Integer, MenuItem> serverMap = new LinkedHashMap<>();
        final Map<Integer, ProxyServerConfig.Node> nodeMap = new LinkedHashMap<>();

        private ServerChooseMenu(PopupMenu menu) {
            menu.add(serverMenu);
        }

        void addServer(final int index, final ProxyServerConfig cfg, final ProxyServerConfig.Node node) {
            MenuItem item;
            if(node.isUse()) {
                item = new MenuItem("√ " + node.getHost() + ":" + node.getPort());
                select = index;
            } else
                item = new MenuItem(node.getHost() + ":" + node.getPort());

            item.addActionListener(e -> {
                if(select != -1) {
                    MenuItem mi = serverMap.get(select); //获取目前选定的MenuItem
                    if (item == mi) //如果选择的是自身
                        return;
                    ProxyServerConfig.Node oldServ = nodeMap.get(select);
                    cfg.setProxyServerUsing(oldServ, false); //将目前启用的代理服务器关闭
                }
                select = index;
                cfg.setProxyServerUsing(node, true); //启用新的代理服务器
            });

            nodeMap.put(index, node);
            serverMap.put(index, item);
            serverMenu.add(item);
        }

        void removeServer(int index) {
            nodeMap.remove(index);
            serverMenu.remove(serverMap.remove(index));
        }

        void initServerItem(ProxyServerConfig cfg) {
            for(MenuItem mi : serverMap.values()) {
                serverMenu.remove(mi);
            }
            nodeMap.clear();
            serverMap.clear();
            select = -1;

            ProxyServerConfig.Node[] nodes = cfg.getProxyServerConfig();
            int i = 0;
            for(ProxyServerConfig.Node node : nodes) {
                addServer(i++, cfg, node);
            }
        }
    }


    private TrayIcon initTray() {
        PopupMenu menu = new PopupMenu();
        MenuItem serverItem = new MenuItem("配置服务器");
        MenuItem socksItem = new MenuItem("Socks5代理设置");
        MenuItem exitItem = new MenuItem("退出");

        this.pacSettingMenu = new PACSettingMenu(menu);
        this.serverChooseMenu = new ServerChooseMenu(menu);

        menu.add(serverItem);
        menu.add(socksItem);
        menu.add(exitItem);

        TrayIcon icon = new TrayIcon(this.icon, "", menu);
        icon.setImageAutoSize(true);
        icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                switch (e.getButton()) {
                    case MouseEvent.BUTTON1 : {
                        getComponent().getModuleByName(ServerSettingModule.NAME, ServerSettingModule.class)
                                .setVisible(true);
                    } break;
                }
            }
        });

        ProxyServerConfig cfg = configManager.getConfig(ProxyServerConfig.DEFAULT_NAME, ProxyServerConfig.class);

        if(cfg == null) {
            configManager.registerConfigEventListener(new ConfigEventListener() {
                @Override
                public void configEvent(ConfigEvent configEvent) {
                    if(configEvent.getEvent().equals(Config.REGISTER_EVENT) && configEvent.getSource() instanceof ProxyServerConfig) {
                        ProxyServerConfig psc = (ProxyServerConfig) configEvent.getSource();
                        serverChooseMenu.initServerItem(psc);
                        configManager.removeConfigEventListener(this);
                    }
                }
            });
        } else {
            serverChooseMenu.initServerItem(cfg);
        }

        ProxyAutoConfig pac = configManager.getConfig(ProxyAutoConfig.DEFAULT_NAME, ProxyAutoConfig.class);
        if(pac == null) {
            configManager.registerConfigEventListener(new ConfigEventListener() {
                @Override
                public void configEvent(ConfigEvent event) {
                    if(event.getEvent().equals(Config.REGISTER_EVENT) && event.getSource() instanceof ProxyAutoConfig) {
                        pacSettingMenu.initProxyAutoConfig((ProxyAutoConfig) event.getSource());
                        configManager.removeConfigEventListener(this);
                    }
                }
            });
        } else {
            pacSettingMenu.initProxyAutoConfig(pac);
        }


        serverItem.addActionListener(e -> getComponent().getModuleByName(ServerSettingModule.NAME,
                ServerSettingModule.class).setVisible(true));

        socksItem.addActionListener(e -> getComponent().getModuleByName(SocksSettingModule.NAME,
                SocksSettingModule.class).setVisiable(true));

        exitItem.addActionListener(e -> {
            SystemTray.getSystemTray().remove(icon);
            getComponent().getParentComponent().stop();
        });

        configManager.registerConfigEventListener(event -> {
            if(event.getEvent().equals(Config.UPDATE_EVENT) && event.getSource() instanceof ProxyServerConfig) {
                ProxyServerConfig psc = (ProxyServerConfig) event.getSource();
                serverChooseMenu.initServerItem(psc);
            }
        });

        try {
            SystemTray.getSystemTray().add(icon);
            return icon;
        } catch (AWTException e) {
            throw new ComponentException(e);
        }
    }
}
