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

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.Client;

import org.eclipse.swt.widgets.Display;

import java.util.Objects;

/**
 * @since 2019.8.13 9:40
 * SWT GUI组件
 */
public class SWTViewComponent extends AbstractComponent<Client> {

    private final Display display;

    /**
     * Socks代理设置页面
     */
    private SocksSettingModule socksSettingModule;

    /**
     * 服务器设置页面
     */
    private ServerSettingModule serverSettingModule;

    /**
     * 主界面
     */
    private MainScreenModule mainScreenModule;

    /**
     * HTTP代理设置页面
     */
    private HttpProxySettingModule httpProxySettingModule;


    public SWTViewComponent(Client parent) {
        super("SWTViewComponent", Objects.requireNonNull(parent));

        ConfigManager<?> configManager = getConfigManager();
        if (configManager.isMacOS()) {
            configManager.setSystemProperties("apple.awt.UIElement", "true");
        }

        try {
            this.display = Display.getDefault();
        } catch (Throwable e) {
            if (Utils.isMacOS()) {
                log.warn("Please use VM argument -XstartOnFirstThread on MacOS");
            }
            throw new Error(e);
        }
    }

    @Override
    protected void initInternal() {
        try {
            addModule(new TrayModule(this, display));
            addModule(this.serverSettingModule = new ServerSettingModule(this, display));
            addModule(this.socksSettingModule = new SocksSettingModule(this, display));
            addModule(this.mainScreenModule = new MainScreenModule(this, display));
            addModule(this.httpProxySettingModule = new HttpProxySettingModule(this));
        } catch (Throwable t) {
            log.error("SWT Thread occur a error", t);
            System.exit(1);
        }
    }

    @Override
    protected void startInternal() {
        parent.setGUITask(() -> {
            try {
                Thread t = Thread.currentThread();
                while (!t.isInterrupted()) {
                    if (!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                display.dispose();
            } catch (RuntimeException | Error t) {
                log.error("SWT Thread occur a error, please submit this issue to GitHub, thanks", t);
                System.exit(1);
            }
        });
    }

    public final Display getDisplay() {
        return display;
    }

    void openSocksSettingUI() {
        socksSettingModule.setVisiable(true);
    }

    void openServerSettingUI() {
        serverSettingModule.setVisiable(true);
    }

    void openMainScreenUI() {
        mainScreenModule.setVisiable(true);
    }

    void openHttpProxySettingUI() {
        httpProxySettingModule.setVisiable(true);
    }

    @Override
    protected void restartInternal() {
        throw new UnsupportedOperationException("Can not restart SWT View Component");
    }
}
