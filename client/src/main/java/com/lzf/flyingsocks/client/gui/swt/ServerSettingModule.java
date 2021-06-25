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

import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.misc.BaseUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;


import static com.lzf.flyingsocks.client.proxy.server.ProxyServerConfig.*;
import static com.lzf.flyingsocks.client.gui.swt.Utils.*;

/**
 * @author lizifan 695199262@qq.com
 * @since 2019.8.17 23:05
 * 服务器设置界面
 */
final class ServerSettingModule extends SwtModule {

    private ServerList serverList;

    private ServerSettingForm serverSettingForm;

    ServerSettingModule(SwtViewComponent component) {
        super(Objects.requireNonNull(component));
    }


    @Override
    protected Shell buildShell() {
        return createShell(display, "swtui.serverconfig.title", loadIcon(), 850, 350);
    }

    @Override
    protected void initial() {
        Composite listComp = new Composite(this.shell, SWT.NONE);
        listComp.setBounds(10, 10, 250, 280);
        this.serverList = new ServerList(listComp);

        Composite formComp = new Composite(this.shell, SWT.NONE);
        formComp.setBounds(270, 10, 570, 280);
        this.serverSettingForm = new ServerSettingForm(formComp);
    }

    private final class ServerList {
        private final List serverList;
        private final Map<Integer, Node> serverMap = new TreeMap<>();
        int select = 0;

        ServerList(Composite composite) {
            serverList = new List(composite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
            serverList.setBounds(0, 0, 250, 280);
            serverList.setToolTipText(i18n("swtui.serverconfig.list.title"));
            serverList.add(i18n("swtui.serverconfig.list.first"), 0);
            serverList.select(0);

            addListSelectionListener(serverList, e -> {
                int idx = serverList.getSelectionIndex();
                select = idx;
                Node n = serverMap.get(idx);
                if (n == null) {
                    serverSettingForm.cleanForm();
                    return;
                }
                serverSettingForm.setHostText(n.getHost());
                serverSettingForm.setPortText(n.getPort());
                serverSettingForm.setCertPortText(n.getCertPort());
                serverSettingForm.setEncrypt(n.getEncryptType());
                serverSettingForm.setAuth(n.getAuthType());

                if (n.getAuthType() == AuthType.USER) {
                    serverSettingForm.setUser(n.getAuthArgument("user"));
                    serverSettingForm.setPass(n.getAuthArgument("pass"));
                } else {
                    serverSettingForm.setPass(n.getAuthArgument("password"));
                }

            });

            flush(false);
            operator.registerProxyServerConfigListener(Config.UPDATE_EVENT, () -> flush(true), false);
        }

        private void flush(boolean clean) {
            if (clean) {
                serverList.remove(1, serverList.getItemCount() - 1);
                serverMap.clear();
            }

            Node[] nodes = operator.getServerNodes();
            int i = 1;
            for (Node node : nodes) {
                serverMap.put(i, node);
                serverList.add(node.getHost() + ":" + node.getPort(), i++);
            }
        }

        private Node selectNode() {
            int select = this.select;
            return select != -1 && select != 0 ? serverMap.get(select) : null;
        }

        boolean contains(String host, int port) {
            for (Node n : serverMap.values()) {
                if (n.getHost().equals(host) && n.getPort() == port) {
                    return true;
                }
            }
            return false;
        }
    }

    private final class ServerSettingForm {
        private static final int ENCRYPT_IDX_NONE = 0;
        private static final int ENCRYPT_IDX_TLS = 1;
        private static final int ENCRYPT_IDX_TLS_CA = 2;
        private static final int AUTH_IDX_NORMAL = 0;
        private static final int AUTH_IDX_USER = 1;

        private final Text host;
        private final Text port;
        private final Text certPort;
        private final Combo encrypt;
        private final Combo auth;
        private final Text user;
        private final Text pass;

        ServerSettingForm(Composite composite) {
            Text host = new Text(composite, SWT.BORDER);
            Text port = new Text(composite, SWT.BORDER);
            Text certPort = new Text(composite, SWT.BORDER);
            Combo encrypt = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
            Combo auth = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);

            Text user = new Text(composite, SWT.BORDER);
            Text pass = new Text(composite, SWT.BORDER | SWT.PASSWORD);

            createLabel(composite, "swtui.serverconfig.form.label.address", 0, 0, 70, 30, SWT.CENTER);
            createLabel(composite, "swtui.serverconfig.form.label.port", 0, 40, 70, 30, SWT.CENTER);
            createLabel(composite, "swtui.serverconfig.form.label.ssl_port", 280, 40, 80, 30, SWT.CENTER);
            createLabel(composite, "swtui.serverconfig.form.label.encrypt_type", 0, 80, 80, 30, SWT.CENTER);
            createLabel(composite, "swtui.serverconfig.form.label.auth_type", 0, 120, 80, 30, SWT.CENTER);
            createLabel(composite, "swtui.serverconfig.form.label.username", 0, 160, 70, 30, SWT.CENTER);
            createLabel(composite, "swtui.serverconfig.form.label.password", 0, 200, 70, 30, SWT.CENTER);

            host.setBounds(90, 0, 460, 30);
            port.setBounds(90, 40, 180, 30);
            certPort.setBounds(370, 40, 180, 30);
            encrypt.setBounds(90, 80, 460, 30);
            auth.setBounds(90, 120, 460, 30);
            user.setBounds(90, 160, 460, 30);
            pass.setBounds(90, 200, 460, 30);

            encrypt.add(i18n("swtui.serverconfig.form.encrypt.none"), ENCRYPT_IDX_NONE);
            encrypt.add(i18n("swtui.serverconfig.form.encrypt.ssl"), ENCRYPT_IDX_TLS);
            encrypt.add(i18n("swtui.serverconfig.form.encrypt.ssl_ca"), ENCRYPT_IDX_TLS_CA);
            auth.add(i18n("swtui.serverconfig.form.auth.normal"), AUTH_IDX_NORMAL);
            auth.add(i18n("swtui.serverconfig.form.auth.user"), AUTH_IDX_USER);

            encrypt.select(ENCRYPT_IDX_NONE);
            auth.select(AUTH_IDX_NORMAL);
            user.setEditable(false);

            addComboSelectionListener(auth, e -> {
                int idx = auth.getSelectionIndex();
                if (idx == AUTH_IDX_NORMAL) {
                    user.setText("");
                    user.setEditable(false);
                } else {
                    user.setEditable(true);
                }
            });

            addComboSelectionListener(encrypt, e -> {
                int idx = encrypt.getSelectionIndex();
                if (idx == ENCRYPT_IDX_NONE || idx == ENCRYPT_IDX_TLS_CA) {
                    certPort.setText("");
                    certPort.setEditable(false);
                } else {
                    certPort.setEditable(true);
                }
            });

            Button save = new Button(composite, SWT.PUSH);
            save.setText(i18n("swtui.serverconfig.form.save"));
            save.setBounds(0, 240, 270, 40);

            addButtonSelectionListener(save, e -> {
                String _host = ServerSettingForm.this.host.getText();
                if (!BaseUtils.isIPv4Address(_host) && !BaseUtils.isHostName(_host)) {
                    showMessageBox(shell, "swtui.serverconfig.notice.error.title", "swtui.serverconfig.notice.error.host_error", SWT.ICON_ERROR | SWT.OK);
                    return;
                }

                String _ports = ServerSettingForm.this.port.getText();
                int _port;
                if (!BaseUtils.isPortString(_ports)) {
                    showMessageBox(shell, "swtui.serverconfig.notice.error.title", "swtui.serverconfig.notice.error.port_error", SWT.ICON_ERROR | SWT.OK);
                    return;
                }

                _port = Integer.parseInt(_ports);
                int en = encrypt.getSelectionIndex();
                String cports = ServerSettingForm.this.certPort.getText();
                int cport = 0;
                if (en == ENCRYPT_IDX_TLS) {
                    if (!BaseUtils.isPortString(cports)) {
                        showMessageBox(shell, "swtui.serverconfig.notice.error.title", "swtui.serverconfig.notice.error.ssl_port_error", SWT.ICON_ERROR | SWT.OK);
                        return;
                    }
                    cport = Integer.parseInt(cports);
                }

                int au = auth.getSelectionIndex();
                String _user = null, pwd;
                pwd = ServerSettingForm.this.pass.getText();
                if (au == 1) {
                    _user = ServerSettingForm.this.user.getText();
                }

                Node n = serverList.selectNode();
                if (n == null) {
                    n = new Node();
                }
                n.setHost(_host);
                n.setPort(_port);
                n.setCertPort(cport);

                if (en == ENCRYPT_IDX_NONE) {
                    n.setEncryptType(EncryptType.NONE);
                } else if (en == ENCRYPT_IDX_TLS) {
                    n.setEncryptType(EncryptType.SSL);
                } else {
                    n.setEncryptType(EncryptType.SSL_CA);
                }

                if (au == AUTH_IDX_NORMAL) {
                    n.setAuthType(AuthType.SIMPLE);
                } else {
                    n.setAuthType(AuthType.USER);
                }

                if (au == 0) {
                    n.setAuthArgument(Collections.singletonMap("password", pwd));
                } else {
                    Map<String, String> map = new HashMap<>(2, 1);
                    map.put("user", _user);
                    map.put("pass", pwd);
                    n.setAuthArgument(map);
                }

                if (serverList.select == -1 || serverList.select == 0) {
                    if (serverList.contains(_host, _port)) {
                        showMessageBox(shell, "swtui.serverconfig.notice.info.title",
                                i18n("swtui.serverconfig.notice.info.server_exists", _host, _port), SWT.ICON_ERROR | SWT.OK);
                        return;
                    }
                    operator.addServerConfig(n);
                    showMessageBox(shell, "swtui.serverconfig.notice.success.title",
                            i18n("swtui.serverconfig.notice.success.server_added", _host, _port), SWT.ICON_INFORMATION | SWT.OK);
                } else {
                    operator.updateServerConfig(n);
                    showMessageBox(shell, "swtui.serverconfig.notice.success.title",
                            i18n("swtui.serverconfig.notice.success.server_updated", _host, _port), SWT.ICON_INFORMATION | SWT.OK);
                }
            });

            Button delete = new Button(composite, SWT.PUSH);
            delete.setText(i18n("swtui.serverconfig.form.delete"));
            delete.setBounds(280, 240, 270, 40);

            addButtonSelectionListener(delete, e -> {
                Node n = serverList.selectNode();
                if (n == null) {
                    showMessageBox(shell, "swtui.serverconfig.notice.info.title", "swtui.serverconfig.notice.info.no_server_select", SWT.ICON_INFORMATION | SWT.OK);
                    return;
                }

                if (n.isUse()) {
                    operator.setProxyServerUsing(n, false);
                }

                operator.removeServer(n);
            });

            this.encrypt = encrypt;
            this.auth = auth;
            this.host = host;
            this.port = port;
            this.certPort = certPort;
            this.user = user;
            this.pass = pass;
        }

        void setHostText(String text) {
            host.setText(text);
        }

        void setPortText(int num) {
            if (num < 0) {
                port.setText("");
            } else {
                port.setText(String.valueOf(num));
            }
        }

        void setCertPortText(int num) {
            if (num < 0) {
                certPort.setText("");
            } else {
                certPort.setText(String.valueOf(num));
            }
        }

        void setEncrypt(EncryptType type) {
            switch (type) {
                case NONE:
                    encrypt.select(0);
                    break;
                case SSL:
                    encrypt.select(1);
                    break;
                case SSL_CA:
                    encrypt.select(2);
                    break;
            }
        }

        void setAuth(AuthType type) {
            switch (type) {
                case SIMPLE: {
                    auth.select(0);
                    user.setEditable(false);
                }
                break;

                case USER: {
                    auth.select(1);
                    user.setEditable(true);
                }
                break;
            }
        }

        void setUser(String text) {
            user.setText(text);
        }

        void setPass(String text) {
            pass.setText(text);
        }

        void cleanForm() {
            setHostText("");
            setPortText(-1);
            setCertPortText(-1);
            setEncrypt(EncryptType.SSL);
            setAuth(AuthType.SIMPLE);
            setUser("");
            setPass("");
        }
    }


}
