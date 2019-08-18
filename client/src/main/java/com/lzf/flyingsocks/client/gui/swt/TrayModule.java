package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.GUIResourceManager;
import com.lzf.flyingsocks.client.proxy.ProxyAutoConfig;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.*;

import java.io.IOException;
import java.util.Objects;

import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.Node;

/**
 * SWT系统托盘实现
 */
final class TrayModule extends AbstractModule<SWTViewComponent> {

    private final Display display;

    private final ClientOperator operator;

    TrayModule(SWTViewComponent component, Display display) {
        super(Objects.requireNonNull(component));
        this.display = Objects.requireNonNull(display);
        this.operator = getComponent().getParentComponent();
        initial();
    }

    private void initial() {
        final Shell shell = new Shell(display);
        shell.setText("flyingsocks");
        final Tray trayTool = display.getSystemTray();

        final TrayItem tray = new TrayItem(trayTool, SWT.NONE);
        final Menu menu = new Menu(shell, SWT.POP_UP);

        tray.addMenuDetectListener(e -> menu.setVisible(true));
        tray.setVisible(true);
        tray.setToolTipText(shell.getText());

        initialPacMenu(shell, menu);

        MenuItem socks = new MenuItem(menu, SWT.PUSH);
        socks.setText("本地Socks5代理设置(&l)");

        MenuItem exit = new MenuItem(menu, SWT.PUSH);
        exit.setText("退出(&x)");

        exit.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                getComponent().getParentComponent().stop();
            }
        });

        try {
            tray.setImage(new Image(display, new ImageData(GUIResourceManager.openSystemTrayImageStream())));
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
            case ProxyAutoConfig.PROXY_PAC: pac1.setSelection(true); break;
            case ProxyAutoConfig.PROXY_NO: pac0.setSelection(true); break;
            case ProxyAutoConfig.PROXY_GLOBAL: pac2.setSelection(true); break;
        }
    }

    private void initialServerMenu(Shell shell, Menu main) {
        MenuItem serv = new MenuItem(main, SWT.CASCADE);
        serv.setText("代理服务器(&s)");
        Node[] nodes = operator.getServerNodes();

        Menu servMenu = new Menu(shell, SWT.DROP_DOWN);
        for (Node node : nodes) {
            MenuItem it = new MenuItem(servMenu, SWT.CASCADE ^ SWT.CHECK);
            it.setText(node.getHost() + ":" + node.getPort());

        }


    }
}
