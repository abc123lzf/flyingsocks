package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.*;

import java.io.IOException;
import java.util.*;

import static com.lzf.flyingsocks.client.proxy.ProxyAutoConfig.*;
import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.Node;

/**
 * SWT系统托盘实现
 */
final class TrayModule extends AbstractModule<SWTViewComponent> {
    private static final String GITHUB_PAGE = "https://github.com/abc123lzf/flyingsocks";
    private static final String ISSUE_PAGE = "https://github.com/abc123lzf/flyingsocks/issues";

    private final Display display;

    private final ClientOperator operator;

    private final Shell shell;

    TrayModule(SWTViewComponent component, Display display) {
        super(Objects.requireNonNull(component));
        this.display = Objects.requireNonNull(display);
        this.operator = getComponent().getParentComponent();
        this.shell = new Shell(display);
        initial();
    }

    private void initial() {
        shell.setText("flyingsocks");
        final Tray trayTool = display.getSystemTray();

        final TrayItem tray = new TrayItem(trayTool, SWT.NONE);
        final Menu menu = new Menu(shell, SWT.POP_UP);

        tray.addMenuDetectListener(e -> menu.setVisible(true));
        tray.setVisible(true);
        tray.setToolTipText(shell.getText());

        createMenuItem(menu, "打开主界面(&m)", e -> belongComponent.openMainScreenUI());

        //服务器选择菜单
        new ServerChooseMenu(shell, menu);

        //PAC设置菜单
        initialPacMenu(shell, menu);

        createMenuItem(menu, "编辑服务器配置...(&e)", e -> belongComponent.openServerSettingUI());
        createMenuSeparator(menu);
        createMenuItem(menu, "本地Socks5代理设置...(&l)", e -> belongComponent.openSocksSettingUI());
        createMenuSeparator(menu);

        initialAboutMenu(shell, menu);
        createMenuItem(menu, "退出(&x)", e -> {
            tray.dispose();
            shell.dispose();
            belongComponent.getParentComponent().stop();
        });

        try {
            tray.setImage(new Image(display, new ImageData(ResourceManager.openSystemTrayImageStream())));
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * PAC设置菜单初始化方法
     */
    private void initialPacMenu(Shell shell, Menu main) {
        MenuItem pac = new MenuItem(main, SWT.CASCADE);
        pac.setText("代理模式(&p)");
        Menu pacMenu = new Menu(shell, SWT.DROP_DOWN);
        pac.setMenu(pacMenu);
        MenuItem pac0 = new MenuItem(pacMenu, SWT.CASCADE ^ SWT.CHECK);
        MenuItem pac1 = new MenuItem(pacMenu, SWT.CASCADE ^ SWT.CHECK);
        MenuItem pac2 = new MenuItem(pacMenu, SWT.CASCADE ^ SWT.CHECK);
        pac0.setText("直连模式");
        pac1.setText("PAC模式");
        pac2.setText("全局模式");

        pac0.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                operator.setProxyMode(0);
                pac0.setSelection(true);
                pac1.setSelection(false);
                pac2.setSelection(false);
            }
        });

        pac1.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                operator.setProxyMode(1);
                pac0.setSelection(false);
                pac1.setSelection(true);
                pac2.setSelection(false);
            }
        });

        pac2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                operator.setProxyMode(2);
                pac0.setSelection(false);
                pac1.setSelection(false);
                pac2.setSelection(true);
            }
        });

        int mode = operator.proxyMode();
        switch (mode) {
            case PROXY_PAC: pac1.setSelection(true); break;
            case PROXY_NO: pac0.setSelection(true); break;
            case PROXY_GLOBAL: pac2.setSelection(true); break;
        }
    }


    private final class ServerChooseMenu {
        private final Menu serverMenu;
        private final Map<Node, MenuItem> menuMap = new HashMap<>();
        private Node usingNode;

        ServerChooseMenu(Shell shell, Menu main) {
            MenuItem serv = new MenuItem(main, SWT.CASCADE);
            serv.setText("代理服务器(&s)");
            this.serverMenu = new Menu(shell, SWT.DROP_DOWN);
            serv.setMenu(this.serverMenu);

            flushNodes(false);
            operator.registerProxyServerConfigListener(Config.UPDATE_EVENT, () -> flushNodes(true), false);
        }

        private void flushNodes(boolean clean) {
            Node[] nodes = operator.getServerNodes();

            if(clean) {
                Set<Node> set = new HashSet<>();
                Collections.addAll(set, nodes);
                Iterator<Map.Entry<Node, MenuItem>> it = menuMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Node, MenuItem> e = it.next();
                    if(!set.contains(e.getKey())) {
                        e.getValue().dispose();
                        it.remove();
                    }
                }

                usingNode = null;
            }

            Set<Node> kSet = menuMap.keySet();

            for (final Node node : nodes) {
                if(kSet.contains(node)) {
                    continue;
                }

                final MenuItem it = new MenuItem(serverMenu, SWT.CASCADE ^ SWT.CHECK);
                it.setText(node.getHost() + ":" + node.getPort());

                menuMap.put(node, it);

                if(node.isUse()) {
                    it.setSelection(true);
                    usingNode = node;
                }

                SelectionListener sl;
                it.addSelectionListener(sl = new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        boolean use = node.isUse();
                        if(use) {
                            operator.setProxyServerUsing(node, false);
                            it.setSelection(false);
                            usingNode = null;
                        } else {
                            Map<Node, Boolean> map = new HashMap<>(4);
                            if(usingNode != null) {
                                menuMap.get(usingNode).setSelection(false);
                                map.put(usingNode, false);
                            }
                            usingNode = node;
                            map.put(node, true);
                            it.setSelection(true);
                            operator.setProxyServerUsing(map);
                        }
                    }
                });

                it.addDisposeListener(e -> it.removeSelectionListener(sl));
            }
        }
    }

    private void initialAboutMenu(Shell shell, Menu main) {
        MenuItem serv = new MenuItem(main, SWT.CASCADE);
        serv.setText("帮助/关于(&a)");
        Menu about = new Menu(shell, SWT.DROP_DOWN);
        serv.setMenu(about);

        createCascadeMenuItem(about, "打开日志目录", e -> operator.openLogDirectory());
        createCascadeMenuItem(about, "清空日志", e -> operator.cleanLogFiles());
        createMenuSeparator(about);
        createCascadeMenuItem(about, "GitHub页面", e -> operator.openBrowser(GITHUB_PAGE));
        createCascadeMenuItem(about, "问题反馈", e -> operator.openBrowser(ISSUE_PAGE));
    }

    /**
     * 创建菜单分隔线
     * @param menu 菜单
     */
    private static void createMenuSeparator(Menu menu) {
        new MenuItem(menu, SWT.SEPARATOR);
    }

    /**
     * 创建菜单项
     * @param menu 菜单
     * @param text 菜单项文本
     * @param listener 点击时监听器
     */
    private static void createMenuItem(Menu menu, String text, SimpleSelectionListener listener) {
        MenuItem it = new MenuItem(menu, SWT.PUSH);
        it.setText(text);
        if(listener != null) {
            it.addSelectionListener(listener);
        }
    }

    /**
     * 创建级联菜单项
     * @param menu 菜单
     * @param text 菜单项文本
     * @param listener 点击时监听器
     */
    private static void createCascadeMenuItem(Menu menu, String text, SimpleSelectionListener listener) {
        MenuItem it = new MenuItem(menu, SWT.CASCADE);
        it.setText(text);
        if(listener != null) {
            it.addSelectionListener(listener);
        }
    }
}
