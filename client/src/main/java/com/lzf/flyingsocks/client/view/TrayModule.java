package com.lzf.flyingsocks.client.view;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.ComponentException;

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
        MenuItem serverItem = new MenuItem("服务器设置");
        MenuItem exitItem = new MenuItem("退出");

        Menu proxyItem = new Menu("代理设置");
        MenuItem proxyItemClose = new MenuItem("关闭");
        MenuItem proxyItemPac = new MenuItem("PAC模式");
        MenuItem proxyItemGlobal = new MenuItem("全局模式");

        proxyItem.add(proxyItemPac);
        proxyItem.add(proxyItemGlobal);
        proxyItem.add(proxyItemClose);

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
}
