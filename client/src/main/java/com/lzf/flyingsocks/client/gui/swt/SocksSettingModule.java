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

import com.lzf.flyingsocks.client.proxy.socks.SocksConfig;
import com.lzf.flyingsocks.misc.BaseUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.util.Objects;

import static com.lzf.flyingsocks.client.gui.swt.Utils.addButtonSelectionListener;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createButton;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createLabel;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createRadio;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createShell;
import static com.lzf.flyingsocks.client.gui.swt.Utils.showMessageBox;

/**
 * @author lizifan 695199262@qq.com
 * @since 2019.9.10
 * Socks5代理设置界面
 */
final class SocksSettingModule extends SwtModule {

    SocksSettingModule(SwtViewComponent component) {
        super(Objects.requireNonNull(component));
    }

    @Override
    protected Shell buildShell() {
        return createShell(display, "swtui.socks5.title", loadIcon(), 600, 250);
    }

    protected void initial() {
        createLabel(shell, "swtui.socks5.form.label.validate", 20, 5, 80, 30, SWT.CENTER);
        createLabel(shell, "swtui.socks5.form.label.username", 20, 40, 80, 30, SWT.CENTER);
        createLabel(shell, "swtui.socks5.form.label.password", 20, 75, 80, 30, SWT.CENTER);
        createLabel(shell, "swtui.socks5.form.label.port", 20, 110, 80, 30, SWT.CENTER);

        Button open = createRadio(shell, "swtui.socks5.form.button.open", 160, 5, 80, 30);
        Button off = createRadio(shell, "swtui.socks5.form.button.close", 250, 5, 80, 30);

        Text user = new Text(shell, SWT.BORDER);
        user.setBounds(160, 40, 380, 30);
        Text pass = new Text(shell, SWT.BORDER | SWT.PASSWORD);
        pass.setBounds(160, 75, 380, 30);
        Text port = new Text(shell, SWT.BORDER);
        port.setBounds(160, 110, 130, 30);

        Button enter = createButton(shell, "swtui.socks5.form.button.enter", 170, 145, 150, 40);
        addButtonSelectionListener(enter, e -> {
            boolean auth = open.getSelection();
            String username = user.getText();
            String password = pass.getText();
            int p;
            if (BaseUtils.isPortString(port.getText())) {
                p = Integer.parseInt(port.getText());
            } else {
                showMessageBox(shell, "swtui.socks5.notice.title", "swtui.socks5.notice.port_error", SWT.ICON_ERROR | SWT.OK);
                return;
            }

            operator.updateSocksProxyAuthentication(p, auth, username, password);
            SocksConfig cfg = operator.getSocksConfig();
            if (cfg.getPort() != p) {
                showMessageBox(shell, "swtui.socks5.notice.title", "swtui.socks5.notice.update_success",
                        SWT.ICON_INFORMATION | SWT.OK);
            } else {
                showMessageBox(shell, "swtui.socks5.notice.title", "swtui.socks5.notice.unchanged",
                        SWT.ICON_INFORMATION | SWT.OK);
            }
        });

        addButtonSelectionListener(open, e -> {
            user.setEditable(true);
            pass.setEditable(true);
        });

        addButtonSelectionListener(off, e -> {
            user.setEditable(false);
            pass.setEditable(false);
        });

        Button cancel = createButton(shell, "swtui.socks5.form.button.cancel", 360, 145, 150, 40);
        addButtonSelectionListener(cancel, e -> setVisiable(false));

        SocksConfig cfg = operator.getSocksConfig();
        if (cfg.isAuth()) {
            open.setSelection(true);
        } else {
            off.setSelection(true);
            user.setEditable(false);
            pass.setEditable(false);
        }

        if (cfg.getUsername() != null) {
            user.setText(cfg.getUsername());
        }

        if (cfg.getPassword() != null) {
            pass.setText(cfg.getPassword());
        }

        port.setText(String.valueOf(cfg.getPort()));
    }
}
