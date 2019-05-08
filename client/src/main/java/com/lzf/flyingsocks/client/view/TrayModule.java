package com.lzf.flyingsocks.client.view;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class TrayModule extends AbstractModule<ViewComponent> {
    static final String NAME = "TrayModule";

    private final TrayIcon tray;

    private final Image icon;

    TrayModule(ViewComponent component, Image icon) {
        super(component, NAME);
        this.icon = icon;
        this.tray = initTray();
    }

    private TrayIcon initTray() {
        PopupMenu menu = new PopupMenu();
        MenuItem serverItem = new MenuItem("配置服务器");
        MenuItem exitItem = new MenuItem("退出");

        Menu proxyItem = new Menu("代理设置 >");
        MenuItem proxyItemClose = new MenuItem("关闭");
        MenuItem proxyItemPac = new MenuItem("PAC模式");
        MenuItem proxyItemGlobal = new MenuItem("全局模式");

        Menu cserverItem = new Menu("选择服务器");

        proxyItem.add(proxyItemPac);
        proxyItem.add(proxyItemGlobal);
        proxyItem.add(proxyItemClose);

        menu.add(cserverItem);
        menu.add(proxyItem);
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
                        initServerItem(cserverItem, psc);
                        cm.removeConfigEventListener(this);
                    }
                }
            });
        } else {
            initServerItem(cserverItem, cfg);
        }


        serverItem.addActionListener(e -> getComponent().getModuleByName(ServerSettingModule.NAME,
                ServerSettingModule.class).setVisible(true));

        exitItem.addActionListener(e -> {
            SystemTray.getSystemTray().remove(icon);
            getComponent().getParentComponent().stop();
        });

        proxyItemPac.addActionListener(e -> {

        });

        try {
            SystemTray.getSystemTray().add(icon);
            return icon;
        } catch (AWTException e) {
            throw new ComponentException(e);
        }
    }

    private static void initServerItem(Menu menu, ProxyServerConfig cfg) {
        ProxyServerConfig.Node[] nodes = cfg.getProxyServerConfig();
        for(ProxyServerConfig.Node node : nodes) {
            MenuItem item = new MenuItem(node.getHost() + ":" + node.getPort());
            menu.add(item);
        }
    }
}
