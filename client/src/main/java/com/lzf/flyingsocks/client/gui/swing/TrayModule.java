package com.lzf.flyingsocks.client.gui.swing;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import com.lzf.flyingsocks.client.proxy.ProxyAutoConfig;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.lzf.flyingsocks.client.proxy.ProxyAutoChecker.*;
import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.Node;

@Deprecated
final class TrayModule extends AbstractModule<SwingViewComponent> {
    private static final Logger log = LoggerFactory.getLogger("PopupMenu");

    static final String NAME = "module.tray";

    private final TrayIcon tray;

    private final Image icon;

    private final ClientOperator operator;

    private PACSettingMenu pacSettingMenu;

    private ServerChooseMenu serverChooseMenu;

    TrayModule(SwingViewComponent component) {
        super(component, NAME);
        this.operator = component.getParentComponent();
        try {
            this.icon = ResourceManager.loadSystemTrayImage();
        } catch (IOException e) {
            log.error("Can not find/load system tray image", e);
            System.exit(1);
            throw new Error(e);
        }
        this.tray = initTray();
    }

    private final class PACSettingMenu {
        int select;
        final MenuItem proxyItemClose = new MenuItem("关闭代理");
        final MenuItem proxyItemPac = new MenuItem("PAC模式");
        final MenuItem proxyItemGlobal = new MenuItem("全局模式");

        private PACSettingMenu(PopupMenu menu) {
            Menu proxyItem = new Menu("代理模式");
            proxyItem.add(proxyItemPac);
            proxyItem.add(proxyItemGlobal);
            proxyItem.add(proxyItemClose);

            proxyItemClose.addActionListener(e -> {
                if(select == PROXY_NO)
                    return;
                select = PROXY_NO;
                operator.setProxyMode(select);
            });

            proxyItemPac.addActionListener(e -> {
                if(select == PROXY_GFW_LIST)
                    return;

                select = PROXY_GFW_LIST;
                operator.setProxyMode(select);
            });

            proxyItemGlobal.addActionListener(e -> {
                if(select == PROXY_GLOBAL)
                    return;

                select = PROXY_GLOBAL;
                operator.setProxyMode(select);
            });

            operator.registerConfigEventListener(event -> {
                if(event.getEvent().equals(Config.UPDATE_EVENT) && event.getSource() instanceof ProxyAutoConfig) {
                    initProxyAutoConfig();
                }
            });

            menu.add(proxyItem);
        }

        private void initProxyAutoConfig() {
            switch (this.select = operator.proxyMode()) {
                case PROXY_NO:
                    proxyItemClose.setLabel("√ 关闭代理");
                    proxyItemPac.setLabel("PAC模式");
                    proxyItemGlobal.setLabel("全局模式");
                    break;
                case PROXY_GFW_LIST:
                    proxyItemClose.setLabel("关闭代理");
                    proxyItemPac.setLabel("√ PAC模式");
                    proxyItemGlobal.setLabel("全局模式");
                    break;
                case PROXY_GLOBAL:
                    proxyItemClose.setLabel("关闭代理");
                    proxyItemPac.setLabel("PAC模式");
                    proxyItemGlobal.setLabel("√ 全局模式");
                    break;
                default:
                    log.error("Unknown ProxyAuto mode.");
            }

        }
    }

    /**
     * 代理服务器选择列表
     */
    private final class ServerChooseMenu {
        int select = -1;
        final Menu serverMenu = new Menu("选择服务器");
        final Map<Integer, MenuItem> serverMap = new LinkedHashMap<>();
        final Map<Integer, Node> nodeMap = new LinkedHashMap<>();

        private ServerChooseMenu(PopupMenu menu) {
            menu.add(serverMenu);
        }

        void addServer(final int index, final ProxyServerConfig.Node node) {
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
                    Node oldServ = nodeMap.get(select);
                    operator.setProxyServerUsing(oldServ, false); //将目前启用的代理服务器关闭
                }
                select = index;
                operator.setProxyServerUsing(node, true); //启用新的代理服务器
            });

            nodeMap.put(index, node);
            serverMap.put(index, item);
            serverMenu.add(item);
        }

        void removeServer(int index) {
            nodeMap.remove(index);
            serverMenu.remove(serverMap.remove(index));
        }

        void initServerItem() {
            for(MenuItem mi : serverMap.values()) {
                serverMenu.remove(mi);
            }
            nodeMap.clear();
            serverMap.clear();
            select = -1;

            Node[] nodes = operator.getServerNodes();
            int i = 0;
            for(Node node : nodes) {
                addServer(i++, node);
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
                        belongComponent.getModuleByName(ServerSettingModule.NAME, ServerSettingModule.class)
                                .setVisible(true);
                    } break;
                }
            }
        });

        serverChooseMenu.initServerItem();
        pacSettingMenu.initProxyAutoConfig();

        serverItem.addActionListener(e -> belongComponent.getModuleByName(ServerSettingModule.NAME,
                ServerSettingModule.class).setVisible(true));

        socksItem.addActionListener(e -> belongComponent.getModuleByName(SocksSettingModule.NAME,
                SocksSettingModule.class).setVisiable(true));

        exitItem.addActionListener(e -> {
            SystemTray.getSystemTray().remove(icon);
            belongComponent.getParentComponent().stop();
        });

        operator.registerConfigEventListener(event -> {
            if(event.getEvent().equals(Config.UPDATE_EVENT) && event.getSource() instanceof ProxyServerConfig) {
                serverChooseMenu.initServerItem();
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
