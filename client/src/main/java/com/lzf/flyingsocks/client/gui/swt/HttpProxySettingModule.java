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
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.GlobalConfig;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import com.lzf.flyingsocks.client.proxy.http.HttpProxyConfig;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Consumer;

import static com.lzf.flyingsocks.client.gui.swt.Utils.addButtonSelectionListener;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createButton;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createLabel;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createRadio;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createShell;
import static com.lzf.flyingsocks.client.gui.swt.Utils.loadImage;
import static com.lzf.flyingsocks.client.gui.swt.Utils.showMessageBox;

/**
 * HTTP本地代理设置界面
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/24 0:50
 */
public class HttpProxySettingModule extends AbstractModule<SWTViewComponent> {

    public static final String NAME = HttpProxySettingModule.class.getSimpleName();

    private final ClientOperator operator;

    private final Shell shell;

    HttpProxySettingModule(SWTViewComponent component) {
        super(Objects.requireNonNull(component), NAME);
        this.operator = component.getParentComponent();

        Image icon;
        try (InputStream is = ResourceManager.openIconImageStream()) {
            icon = loadImage(is);
        } catch (IOException e) {
            throw new Error(e);
        }

        this.shell = createShell(component.getDisplay(), "swtui.http.title", icon, 600, 280);
        initial();
    }

    private void initial() {
        createLabel(shell, "swtui.http.form.label.switch", 20, 5, 80, 30, SWT.CENTER);
        createLabel(shell, "swtui.http.form.label.port", 20, 40, 80, 30, SWT.CENTER);
        createLabel(shell, "swtui.http.form.label.validate", 20, 75, 80, 30, SWT.CENTER);
        createLabel(shell, "swtui.http.form.label.username", 20, 110, 80, 30, SWT.CENTER);
        createLabel(shell, "swtui.http.form.label.password", 20, 145, 80, 30, SWT.CENTER);

        Composite switchComp = new Composite(shell, SWT.NONE);
        switchComp.setBounds(20, 5, 380, 30);
        Button openRadio = createRadio(switchComp, "swtui.http.form.button.switch_open", 140, 5, 80, 30);
        Button closeRadio = createRadio(switchComp, "swtui.http.form.button.switch_close", 230, 5, 80, 30);
        addButtonSelectionListener(openRadio, e -> {
            openRadio.setSelection(true);
            closeRadio.setSelection(false);
        });
        addButtonSelectionListener(closeRadio, e -> {
            openRadio.setSelection(false);
            closeRadio.setSelection(true);
        });

        Text portText = new Text(shell, SWT.BORDER);
        portText.setBounds(160, 40, 380, 30);

        Composite authComp = new Composite(shell, SWT.NONE);
        authComp.setBounds(20, 75, 380, 30);
        Button authOpenRadio = createRadio(authComp, "swtui.http.form.button.validate_open", 140, 0, 80, 30);
        Button authCloseRadio = createRadio(authComp, "swtui.http.form.button.validate_close", 230, 0, 80, 30);
        addButtonSelectionListener(authCloseRadio, e -> {
            authCloseRadio.setSelection(true);
            authOpenRadio.setSelection(false);
        });
        addButtonSelectionListener(authOpenRadio, e -> {
            authCloseRadio.setSelection(false);
            authOpenRadio.setSelection(true);
        });

        Text userText = new Text(shell, SWT.BORDER);
        userText.setBounds(160, 110, 380, 30);

        Text passText = new Text(shell, SWT.BORDER | SWT.PASSWORD);
        passText.setBounds(160, 145, 380, 30);

        Button enterBtn = createButton(shell, "swtui.http.form.button.enter", 140, 180, 150, 35);
        Button cancelBtn = createButton(shell, "swtui.http.form.button.cancel", 330, 180, 150, 35);

        addButtonSelectionListener(enterBtn, e -> {
            boolean open = openRadio.getSelection();
            boolean auth = authOpenRadio.getSelection();
            int port;
            try {
                port = Integer.parseInt(portText.getText());
            } catch (NumberFormatException nfe) {
                showMessageBox(shell, "swtui.http.notice.error.title", "swtui.http.notice.error.port_error", SWT.ICON_ERROR | SWT.OK);
                return;
            }
            String username = userText.getText();
            String password = passText.getText();

            if (auth && StringUtils.isAnyBlank(username, password)) {
                showMessageBox(shell, "swtui.http.notice.error.title", "swtui.http.notice.error.auth_error", SWT.ICON_ERROR | SWT.OK);
                return;
            }

            operator.updateHttpProxyConfig(open, port, auth, username, password);
            showMessageBox(shell, "swtui.http.notice.error.title", "swtui.http.notice.update_success", SWT.ICON_INFORMATION | SWT.OK);
        });

        if (operator.isHttpProxyOpen()) {
            openRadio.setSelection(true);
        } else {
            closeRadio.setSelection(true);
        }

        HttpProxyConfig cfg = operator.getHttpProxyConfig();
        Consumer<HttpProxyConfig> updater = config -> {
            if (config == null) {
                closeRadio.setSelection(true);
                authCloseRadio.setSelection(true);
                return;
            }

            portText.setText(String.valueOf(config.getBindPort()));
            if (config.getUsername() != null) {
                userText.setText(config.getUsername());
            }
            if (config.getPassword() != null) {
                passText.setText(config.getPassword());
            }
            if (config.isAuth()) {
                authOpenRadio.setSelection(true);
            } else {
                authCloseRadio.setSelection(true);
            }
        };

        if (cfg != null) {
            updater.accept(cfg);
        } else {
            closeRadio.setSelection(true);
            authCloseRadio.setSelection(true);
        }

        addButtonSelectionListener(cancelBtn, e -> {
            if (operator.isHttpProxyOpen()) {
                openRadio.setSelection(true);
            } else {
                closeRadio.setSelection(true);
            }
            HttpProxyConfig hpc = operator.getHttpProxyConfig();
            updater.accept(hpc);
            setVisiable(false);
        });

        operator.registerConfigEventListener(event -> {
            if (Config.UPDATE_EVENT.equals(event.getEvent()) && event.getSource() instanceof HttpProxyConfig) {
                HttpProxyConfig hpc = (HttpProxyConfig) event.getSource();
                updater.accept(hpc);
            }
        });

        operator.registerConfigEventListener(event -> {
            if (Config.UPDATE_EVENT.equals(event.getEvent()) && event.getSource() instanceof GlobalConfig) {
                GlobalConfig gc = (GlobalConfig) event.getSource();
                if (gc.isEnableHttpProxy()) {
                    openRadio.setSelection(true);
                } else {
                    closeRadio.setSelection(true);
                }
            }
        });
    }

    void setVisiable(boolean visiable) {
        shell.setVisible(visiable);
    }
}
