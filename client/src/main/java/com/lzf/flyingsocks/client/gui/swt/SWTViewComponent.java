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
public class SwtViewComponent extends AbstractComponent<Client> {

    private final Display display;

    public SwtViewComponent(Client parent) {
        super("SwtViewComponent", Objects.requireNonNull(parent));

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
            addModule(new TrayModule(this));
            addModule(new ServerSettingModule(this));
            addModule(new SocksSettingModule(this));
            addModule(new MainScreenModule(this));
            addModule(new HttpProxySettingModule(this));
        } catch (Throwable t) {
            log.error("SWT Thread occur a error", t);
            Client.exitWithNotify(1, "exitmsg.swt_view.init_failure", t.getMessage());
        }
    }

    @Override
    protected void startInternal() {
        parent.setGUITask(() -> {
            try {
                Thread t = Thread.currentThread();
                t.setName("SWT-UI-Thread");
                while (!t.isInterrupted()) {
                    if (!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                display.dispose();
            } catch (RuntimeException | Error t) {
                log.error("An error occur in SWT-UI-Thread", t);
                Client.exitWithNotify(1, "exitmsg.swt_view.run_error", t.getMessage());
            }
        });
    }

    Display getDisplay() {
        return display;
    }

    void openSocksSettingUI() {
        Class<SocksSettingModule> klass = SocksSettingModule.class;
        getModuleByName(SwtModule.getModuleName(klass), klass).setVisiable(true);
    }

    void openServerSettingUI() {
        Class<ServerSettingModule> klass = ServerSettingModule.class;
        getModuleByName(SwtModule.getModuleName(klass), klass).setVisiable(true);
    }

    void openMainScreenUI() {
        Class<MainScreenModule> klass = MainScreenModule.class;
        getModuleByName(SwtModule.getModuleName(klass), klass).setVisiable(true);
    }

    void openHttpProxySettingUI() {
        Class<HttpProxySettingModule> klass = HttpProxySettingModule.class;
        getModuleByName(SwtModule.getModuleName(klass), klass).setVisiable(true);
    }

    @Override
    protected void restartInternal() {
        throw new UnsupportedOperationException("Can not restart SWT View Component");
    }
}
