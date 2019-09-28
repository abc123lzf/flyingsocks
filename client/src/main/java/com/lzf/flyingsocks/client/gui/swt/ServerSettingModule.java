package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import com.lzf.flyingsocks.util.BaseUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.*;

/**
 * @create 2019.8.17 23:05
 * @description 服务器设置界面
 */
final class ServerSettingModule extends AbstractModule<SWTViewComponent> {

    private static final Logger log = LoggerFactory.getLogger("ServerSettingUI");

    private final Display display;

    private final Shell shell;

    private final ClientOperator operator;

    private final ServerList serverList;

    private final ServerSettingForm serverSettingForm;

    ServerSettingModule(SWTViewComponent component, Display display) {
        super(Objects.requireNonNull(component));
        this.display = display;
        this.operator = component.getParentComponent();
        this.shell = initialShell();
        this.serverList = new ServerList();
        this.serverSettingForm = new ServerSettingForm();
    }


    private final class ServerList {
        private final List serverList;
        private final Map<Integer, Node> serverMap = new TreeMap<>();
        int select = -1;

        ServerList() {
            serverList = new List(shell, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
            serverList.setBounds(10, 10, 250, 475);
            serverList.setToolTipText("服务器列表");

            serverList.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int idx = serverList.getSelectionIndex();
                    Node n = serverMap.get(idx);
                    if(n == null) {
                        return;
                    }
                    serverSettingForm.setHostText(n.getHost());
                    serverSettingForm.setPortText(n.getPort());
                    serverSettingForm.setCertPortText(n.getCertPort());
                    serverSettingForm.setEncrypt(n.getEncryptType());
                    serverSettingForm.setAuth(n.getAuthType());

                    if(n.getAuthType() == AuthType.USER) {
                        serverSettingForm.setUser(n.getAuthArgument("user"));
                        serverSettingForm.setPass(n.getAuthArgument("pass"));
                    } else {
                        serverSettingForm.setPass(n.getAuthArgument("password"));
                    }
                    select = idx;
                }
            });
            flush(false);
            operator.registerProxyServerConfigListener(Config.UPDATE_EVENT, () -> flush(true), false);
        }

        private void flush(boolean clean) {
            if(clean) {
                serverList.removeAll();
                serverMap.clear();
            }

            Node[] nodes = operator.getServerNodes();
            int i = 0;
            for (Node node : nodes) {
                serverMap.put(i, node);
                serverList.add(node.getHost() + ":" + node.getPort(), i++);
            }
        }

        Node selectNode() {
            return select != -1 ? serverMap.get(select) : null;
        }
    }

    private final class ServerSettingForm {
        private final Text host;
        private final Text port;
        private final Text certPort;
        private final Combo encrypt;
        private final Combo auth;
        private final Text user;
        private final Text pass;

        ServerSettingForm() {
            Text host = new Text(shell, SWT.BORDER);
            Text port = new Text(shell, SWT.BORDER);
            Text certPort = new Text(shell, SWT.BORDER);
            Combo encrypt = new Combo(shell, SWT.DROP_DOWN | SWT.READ_ONLY);
            Combo auth = new Combo(shell, SWT.DROP_DOWN | SWT.READ_ONLY);

            Text user = new Text(shell, SWT.BORDER);
            Text pass = new Text(shell, SWT.BORDER | SWT.PASSWORD);

            createLabel("地址", 270, 10, 70, 30);
            createLabel("端口", 270, 50, 70, 30);
            createLabel("证书端口", 550, 50, 80, 30);
            createLabel("加密方式", 270, 90, 80, 30);
            createLabel("认证方式", 270, 130, 80, 30);
            createLabel("用户名", 270, 170, 70, 30);
            createLabel("密码", 270, 210, 70, 30);

            host.setBounds(360, 10, 460, 30);
            port.setBounds(360, 50, 180, 30); certPort.setBounds(640, 50, 180, 30);
            encrypt.setBounds(360, 90, 460, 30);
            auth.setBounds(360, 130, 460, 30);
            user.setBounds(360,170, 460, 30);
            pass.setBounds(360, 210, 460, 30);

            encrypt.add("无加密", 0);
            encrypt.add("TLS/SSL", 1);
            auth.add("普通认证", 0);
            auth.add("用户认证", 1);

            encrypt.select(0);
            auth.select(0);

            auth.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int idx = auth.getSelectionIndex();
                    if(idx == 0) {
                        user.setText("");
                        user.setEditable(false);
                    } else {
                        user.setEditable(true);
                    }
                }
            });

            encrypt.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int idx = encrypt.getSelectionIndex();
                    if(idx == 0) {
                        certPort.setText("");
                        certPort.setEditable(false);
                    } else {
                        certPort.setEditable(true);
                    }
                }
            });

            Button save = new Button(shell, SWT.PUSH);
            save.setText("保存");
            save.setBounds(270, 250, 270, 60);

            try (InputStream is = ResourceManager.openSaveIconImageStream()) {
                save.setImage(new Image(display, is));
            } catch (IOException e) {
                log.warn("Can not read save-icon image.", e);
            }
            save.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String host = ServerSettingForm.this.host.getText();
                    if(!BaseUtils.isIPv4Address(host) && !BaseUtils.isHostName(host)) {
                        showMessageBox("错误", "服务器主机名格式有误:需要为IPv4地址或是合法主机名/域名", SWT.ICON_ERROR | SWT.OK);
                        return;
                    }

                    String ports = ServerSettingForm.this.port.getText();
                    int port;
                    if(!BaseUtils.isPortString(ports)) {
                        showMessageBox("错误", "端口号有误,必须为1~65535之间的数字", SWT.ICON_ERROR | SWT.OK);
                        return;
                    }

                    port = Integer.parseInt(ports);
                    int en = encrypt.getSelectionIndex();
                    String cports = ServerSettingForm.this.certPort.getText();
                    int cport = 0;
                    if(en == 1) {
                        if(!BaseUtils.isPortString(cports)) {
                            showMessageBox("错误", "证书端口号有误,必须为1~65535之间的数字", SWT.ICON_ERROR | SWT.OK);
                            return;
                        }
                        cport = Integer.parseInt(cports);
                    }

                    int au = auth.getSelectionIndex();
                    String user = null, pwd;
                    pwd = ServerSettingForm.this.pass.getText();
                    if(au == 1) {
                        user = ServerSettingForm.this.user.getText();
                    }

                    Node n = serverList.selectNode();
                    if(n == null) {
                        n = new Node();
                    }
                    n.setHost(host);
                    n.setPort(port);
                    n.setCertPort(cport);
                    n.setEncryptType(en == 1 ? EncryptType.SSL : EncryptType.NONE);
                    n.setAuthType(au == 1 ? AuthType.USER : AuthType.SIMPLE);
                    if(au == 0) {
                        n.setAuthArgument(Collections.singletonMap("password", pwd));
                    } else {
                        Map<String, String> map = new HashMap<>(2, 1);
                        map.put("user", user);
                        map.put("pass", pwd);
                        n.setAuthArgument(map);
                    }

                    if(serverList.select == -1) {
                        operator.addServerConfig(n);
                        showMessageBox("成功", "成功添加服务器配置 " + host + ":" + port, SWT.ICON_INFORMATION | SWT.OK);
                    } else {
                        operator.updateServerConfig(n);
                        showMessageBox("成功", "成功修改服务器配置 " + host + ":" + port, SWT.ICON_INFORMATION | SWT.OK);
                    }
                }
            });

            Button delete = new Button(shell, SWT.PUSH);
            delete.setText("删除");
            delete.setBounds(550, 250, 270, 60);

            try(InputStream is = ResourceManager.openDeleteIconImageStream()) {
                delete.setImage(new Image(display, is));
            } catch (IOException e) {
                log.warn("Can not read delete-icon image.", e);
            }

            delete.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    super.widgetSelected(e);
                }
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
            port.setText(String.valueOf(num));
        }

        void setCertPortText(int num) {
            certPort.setText(String.valueOf(num));
        }

        void setEncrypt(EncryptType type) {
            switch (type) {
                case NONE: encrypt.select(0); break;
                case SSL: encrypt.select(1); break;
            }
        }

        void setAuth(AuthType type) {
            switch (type) {
                case SIMPLE: {
                    auth.select(0);
                    user.setEditable(false);
                } break;

                case USER: {
                    auth.select(1);
                    user.setEditable(true);
                } break;
            }
        }

        void setUser(String text) {
            user.setText(text);
        }

        void setPass(String text) {
            pass.setText(text);
        }
    }


    private Shell initialShell() {
        final Shell shell = new Shell(display);
        shell.setText("服务器设置");
        shell.setSize(850, 550);

        try (InputStream is = ResourceManager.openIconImageStream()) {
            shell.setImage(new Image(display, is));
        } catch (IOException e) {
            throw new Error(e);
        }

        shell.addListener(SWT.Close, e -> {
            setVisiable(false);
            e.doit = false;
        });

        shell.setVisible(false);
        return shell;
    }

    void setVisiable(boolean visiable) {
        shell.setVisible(visiable);
    }

    private void createLabel(String text, int x, int y, int width, int height) {
        Label l = new Label(shell, SWT.CENTER);
        l.setBounds(x, y, width, height);
        l.setText(text);
    }

    private void showMessageBox(String title, String content, int setting) {
        MessageBox box = new MessageBox(shell, setting);
        box.setText(title);
        box.setMessage(content);
        box.open();
    }
}
