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
package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.ResourceManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import java.io.IOException;
import java.util.Objects;

import static com.lzf.flyingsocks.client.gui.swt.Utils.*;
import static com.lzf.flyingsocks.client.proxy.ProxyAutoChecker.*;

/**
 * @author lizifan 695199262@qq.com
 * SWT系统托盘实现
 */
final class TrayModule extends AbstractModule<SWTViewComponent> {
    private static final String GITHUB_PAGE = "https://github.com/abc123lzf/flyingsocks";
    private static final String ISSUE_PAGE = "https://github.com/abc123lzf/flyingsocks/issues";

    private final Display display;

    private final ClientOperator operator;

    private final Shell shell;

    TrayModule(SWTViewComponent component) {
        super(Objects.requireNonNull(component));
        this.display = component.getDisplay();
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

        createMenuItem(menu, "swtui.tray.item.open_main_screen_ui", e -> belongComponent.openMainScreenUI());

        //PAC设置菜单
        initialPacMenu(shell, menu);

        createMenuItem(menu, "swtui.tray.item.server_config_ui", e -> belongComponent.openServerSettingUI());
        createMenuSeparator(menu);
        createMenuItem(menu, "swtui.tray.item.socks5_config_ui", e -> belongComponent.openSocksSettingUI());
        createMenuItem(menu, "swtui.tray.item.http_config_ui", e -> belongComponent.openHttpProxySettingUI());
        createMenuSeparator(menu);

        initialAboutMenu(shell, menu);
        createMenuItem(menu, "swtui.tray.item.exit", e -> {
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
        pac.setText(i18n("swtui.tray.item.proxy_mode"));
        Menu pacMenu = new Menu(shell, SWT.DROP_DOWN);
        pac.setMenu(pacMenu);
        MenuItem pac0 = new MenuItem(pacMenu, SWT.CASCADE ^ SWT.CHECK);
        MenuItem pac1 = new MenuItem(pacMenu, SWT.CASCADE ^ SWT.CHECK);
        MenuItem pac3 = new MenuItem(pacMenu, SWT.CASCADE ^ SWT.CHECK);
        MenuItem pac2 = new MenuItem(pacMenu, SWT.CASCADE ^ SWT.CHECK);

        pac0.setText(i18n("swtui.tray.item.proxy_mode.no_proxy"));
        pac1.setText(i18n("swtui.tray.item.proxy_mode.gfwlist"));
        pac2.setText(i18n("swtui.tray.item.proxy_mode.global"));
        pac3.setText(i18n("swtui.tray.item.proxy_mode.ipwhitelist"));

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
            case PROXY_GFW_LIST:
                pac1.setSelection(true);
                break;
            case PROXY_NO:
                pac0.setSelection(true);
                break;
            case PROXY_GLOBAL:
                pac2.setSelection(true);
                break;
            case PROXY_NON_CN:
                pac3.setSelection(true);
                break;
        }
    }

    private void initialAboutMenu(Shell shell, Menu main) {
        MenuItem serv = new MenuItem(main, SWT.CASCADE);
        serv.setText(i18n("swtui.tray.item.help"));
        Menu about = new Menu(shell, SWT.DROP_DOWN);
        serv.setMenu(about);

        createCascadeMenuItem(about, "swtui.tray.item.help.open_config_dir", e -> operator.openConfigDirectory());
        createCascadeMenuItem(about, "swtui.tray.item.help.open_log_dir", e -> operator.openLogDirectory());
        createCascadeMenuItem(about, "swtui.tray.item.help.clean_log", e -> operator.cleanLogFiles());
        createMenuSeparator(about);
        createCascadeMenuItem(about, "swtui.tray.item.help.open_github", e -> operator.openBrowser(GITHUB_PAGE));
        createCascadeMenuItem(about, "swtui.tray.item.help.open_issue", e -> operator.openBrowser(ISSUE_PAGE));
    }

}
