package com.lzf.flyingsocks.client.view;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

class TrayModule extends AbstractModule<ViewComponent> {
    static final String NAME = "TrayModule";

    private final TrayIcon tray;

    private final Image icon;

    private PACSettingMenu pacSettingMenu;

    private ServerChooseMenu serverChooseMenu;

    TrayModule(ViewComponent component, Image icon) {
        super(component, NAME);
        this.icon = icon;
        this.tray = initTray();
    }

    private class PACSettingMenu {
        final MenuItem proxyItemClose = new MenuItem("关闭代理");
        final MenuItem proxyItemPac = new MenuItem("PAC模式");
        final MenuItem proxyItemGlobal = new MenuItem("全局模式");

        private PACSettingMenu(PopupMenu menu) {
            Menu proxyItem = new Menu("代理设置");
            proxyItem.add(proxyItemPac);
            proxyItem.add(proxyItemGlobal);
            proxyItem.add(proxyItemClose);
            menu.add(proxyItem);
        }
    }


    private class ServerChooseMenu {
        final Menu serverMenu = new Menu("选择服务器");
        final Map<Integer, MenuItem> serverMap = new LinkedHashMap<>();

        private ServerChooseMenu(PopupMenu menu) {
            menu.add(serverMenu);
        }

        void addServer(int index, String host, int port) {
            MenuItem item = new MenuItem(host + ":" + port);
            serverMap.put(index, item);
            serverMenu.add(item);
        }

        void removeServer(int index) {
            serverMenu.remove(serverMap.remove(index));
        }

        void initServerItem(ProxyServerConfig cfg) {
            for(Integer i : serverMap.keySet()) {
                removeServer(i);
            }

            ProxyServerConfig.Node[] nodes = cfg.getProxyServerConfig();
            int i = 0;
            for(ProxyServerConfig.Node node : nodes) {
                addServer(i++, node.getHost(), node.getPort());
            }
        }
    }


    private TrayIcon initTray() {
        PopupMenu menu = new PopupMenu();
        MenuItem serverItem = new MenuItem("配置服务器");
        MenuItem exitItem = new MenuItem("退出");

        this.pacSettingMenu = new PACSettingMenu(menu);
        this.serverChooseMenu = new ServerChooseMenu(menu);

        menu.add(serverItem);
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

        ConfigManager<?> cm = getComponent().getParentComponent().getConfigManager();
        ProxyServerConfig cfg = cm.getConfig(ProxyServerConfig.DEFAULT_NAME, ProxyServerConfig.class);

        if(cfg == null) {
            cm.registerConfigEventListener(new ConfigEventListener() {
                @Override
                public void configEvent(ConfigEvent configEvent) {
                    if(configEvent.getEvent().equals(Config.REGISTER_EVENT) && configEvent.getSource() instanceof ProxyServerConfig) {
                        ProxyServerConfig psc = (ProxyServerConfig) configEvent.getSource();
                        serverChooseMenu.initServerItem(psc);
                        cm.removeConfigEventListener(this);
                    }
                }
            });
        } else {
            serverChooseMenu.initServerItem(cfg);
        }


        serverItem.addActionListener(e -> getComponent().getModuleByName(ServerSettingModule.NAME,
                ServerSettingModule.class).setVisible(true));

        exitItem.addActionListener(e -> {
            SystemTray.getSystemTray().remove(icon);
            getComponent().getParentComponent().stop();
        });

        cm.registerConfigEventListener(event -> {
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
