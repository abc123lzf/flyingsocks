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
import org.eclipse.swt.widgets.Shell;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static com.lzf.flyingsocks.client.gui.swt.Utils.createLabel;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createShell;
import static com.lzf.flyingsocks.client.gui.swt.Utils.loadImage;

/**
 * HTTP本地代理设置界面
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/24 0:50
 */
public class HttpProxySettingModule extends AbstractModule<SWTViewComponent> {

    private final ClientOperator operator;

    private final Shell shell;

    HttpProxySettingModule(SWTViewComponent component) {
        super(Objects.requireNonNull(component), "HttpProxySettingModule");
        this.operator = component.getParentComponent();

        Image icon;
        try (InputStream is = ResourceManager.openIconImageStream()) {
            icon = loadImage(is);
        } catch (IOException e) {
            throw new Error(e);
        }

        this.shell = createShell(component.getDisplay(), "Socks5本地代理设置", icon, 600, 250);
    }

    private void initial() {
        createLabel(shell, "开关", 20, 5, 80, 30, SWT.CENTER);
        //createLabel(shell, "", );
    }

    void setVisiable(boolean visiable) {
        shell.setVisible(visiable);
    }
}
