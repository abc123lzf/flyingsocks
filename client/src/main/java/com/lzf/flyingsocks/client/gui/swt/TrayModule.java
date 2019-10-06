package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.*;

import java.io.IOException;
import java.util.Objects;

import static com.lzf.flyingsocks.client.gui.swt.Utils.*;
import static com.lzf.flyingsocks.client.proxy.ProxyAutoConfig.PROXY_NO;
import static com.lzf.flyingsocks.client.proxy.ProxyAutoConfig.PROXY_GFW_LIST;
import static com.lzf.flyingsocks.client.proxy.ProxyAutoConfig.PROXY_GLOBAL;
import static com.lzf.flyingsocks.client.proxy.ProxyAutoConfig.PROXY_NON_CN;

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
        shell.setText("flyingsocks");

        initial();
    }

    private void initial() {
        final Tray trayTool = display.getSystemTray();

        final TrayItem tray = new TrayItem(trayTool, SWT.NONE);
        final Menu menu = new Menu(shell, SWT.POP_UP);

        tray.addMenuDetectListener(e -> menu.setVisible(true));

        tray.setVisible(true);
        tray.setToolTipText(shell.getText());

        createMenuItem(menu, "打开主界面(&m)", e -> belongComponent.openMainScreenUI());

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
        MenuItem pac3 = new MenuItem(pacMenu, SWT.CASCADE ^ SWT.CHECK);
        MenuItem pac2 = new MenuItem(pacMenu, SWT.CASCADE ^ SWT.CHECK);

        pac0.setText("不代理");
        pac1.setText("GFW List模式");
        pac2.setText("全局代理模式");
        pac3.setText("仅代理境外地址");

        addMenuItemSelectionListener(pac0, e -> {
            operator.setProxyMode(PROXY_NO);
            pac0.setSelection(true);
            pac1.setSelection(false);
            pac2.setSelection(false);
            pac3.setSelection(false);
        });

        addMenuItemSelectionListener(pac1, e -> {
            operator.setProxyMode(PROXY_GFW_LIST);
            pac0.setSelection(false);
            pac1.setSelection(true);
            pac2.setSelection(false);
            pac3.setSelection(false);
        });

        addMenuItemSelectionListener(pac2, e -> {
            operator.setProxyMode(PROXY_GLOBAL);
            pac0.setSelection(false);
            pac1.setSelection(false);
            pac2.setSelection(true);
            pac3.setSelection(false);
        });

        addMenuItemSelectionListener(pac3, e -> {
            operator.setProxyMode(PROXY_NON_CN);
            pac0.setSelection(false);
            pac1.setSelection(false);
            pac2.setSelection(false);
            pac3.setSelection(true);
        });

        int mode = operator.proxyMode();
        switch (mode) {
            case PROXY_GFW_LIST: pac1.setSelection(true); break;
            case PROXY_NO: pac0.setSelection(true); break;
            case PROXY_GLOBAL: pac2.setSelection(true); break;
            case PROXY_NON_CN: pac3.setSelection(true); break;
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

}
