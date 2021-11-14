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
package com.lzf.flyingsocks.client.gui.swing;

import com.lzf.flyingsocks.client.gui.ResourceManager;

import javax.imageio.ImageIO;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/8/21 9:50 下午
 */
final class SystemTrayModule extends SwingModule {

    private static final String GITHUB_PAGE = "https://github.com/abc123lzf/flyingsocks";

    private static final String ISSUE_PAGE = "https://github.com/abc123lzf/flyingsocks/issues";

    public SystemTrayModule(SwingViewComponent component) {
        super(Objects.requireNonNull(component));
    }

    @Override
    protected void initial() throws Exception {
        if (!SystemTray.isSupported()) {
            throw new Error("SystemTray not support!");
        }

        SystemTray tray = SystemTray.getSystemTray();
        TrayIcon icon = new TrayIcon(ImageIO.read(ResourceManager.openSystemTrayImageStream()), "flyingsocks");

        JPopupMenu menu = new JPopupMenu();
        createMenuItem(menu, "打开主界面", null);
        initialPacMenu(menu);
        createMenuItem(menu, "服务器配置", () -> belongComponent.getModuleByName(
                "ServerConfigureModule", SwingModule.class).setVisiable(true));
        menu.addSeparator();
        createMenuItem(menu, "本地代理设置", () -> belongComponent.getModuleByName(
                "LocalProxyConfigureModule", SwingModule.class).setVisiable(true));
        menu.addSeparator();
        initialAboutMenu(menu);
        createMenuItem(menu, "退出", () -> belongComponent.getParentComponent().stop());


        icon.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menu.setLocation(e.getLocationOnScreen());
                    menu.setInvoker(menu);
                    menu.setVisible(true);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menu.setLocation(e.getLocationOnScreen());
                    menu.setInvoker(menu);
                    menu.setVisible(true);
                }
            }
        });

        tray.add(icon);
    }

    @Override
    public void setVisiable(boolean visiable) {
        // NOOP
    }


    private void initialAboutMenu(JPopupMenu parent) {
        JMenu menu = createCascadeMenu(parent, "帮助/关于");
        createMenuItem(menu, "打开配置文件目录", clientOperator::openConfigDirectory);
        createMenuItem(menu, "打开GitHub页面", () -> clientOperator.openBrowser(GITHUB_PAGE));
        createMenuItem(menu, "问题反馈", () -> clientOperator.openBrowser(ISSUE_PAGE));
    }

    private void initialPacMenu(JPopupMenu parent) {
        JMenu menu = createCascadeMenu(parent, "PAC模式");
        createCheckboxMenuItem(menu, "不代理", null);
        createCheckboxMenuItem(menu, "GFW List", null);
        createCheckboxMenuItem(menu, "全局模式", null);
        createCheckboxMenuItem(menu, "IP白名单", null);
    }
}
