package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.EncryptType;
import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.AuthType;
import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.Node;

/**
 * @create 2019.8.17 23:05
 * @description 服务器设置界面
 */
final class ServerSettingModule extends AbstractModule<SWTViewComponent> {

    private final Display display;

    private final Shell shell;

    private final ClientOperator operator;

    ServerSettingModule(SWTViewComponent component, Display display) {
        super(Objects.requireNonNull(component));
        this.display = display;
        this.operator = component.getParentComponent();
        this.shell = initialShell();
        initialComponent();
    }


    private final class ServerList {
        private final List serverList;
        private final Map<Integer, Node> serverMap = new LinkedHashMap<>(8, 1);

        ServerList() {
            serverList = new List(shell, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
            serverList.setBounds(10, 10, 250, 500);
            serverList.setToolTipText("服务器列表");
            serverList.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {

                }

                @Override
                public void focusLost(FocusEvent e) {

                }
            });
            flush(false);

            operator.registerConfigEventListener(e -> {
                if(e.getSource() instanceof ProxyServerConfig && e.getEvent().equals(Config.UPDATE_EVENT)) {
                    flush(true);
                }
            });
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
            Combo encrypt = new Combo(shell, SWT.DROP_DOWN);
            Combo auth = new Combo(shell, SWT.DROP_DOWN);

            Text user = new Text(shell, SWT.BORDER);
            Text pass = new Text(shell, SWT.BORDER);

            host.setBounds(360, 10, 460, 40);
            port.setBounds(360, 65, 180, 40); certPort.setBounds(620, 65, 200, 40);
            encrypt.setBounds(360, 120, 460, 50);
            auth.setBounds(360, 175, 460, 50);
            user.setBounds(360,230, 460, 40);
            pass.setBounds(360, 285, 460, 40);

            encrypt.add("无加密", 0);
            encrypt.add("TLS/SSL", 1);
            auth.add("普通认证", 0);
            auth.add("用户认证", 1);

            encrypt.select(0);
            auth.select(0);

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
                case SIMPLE: encrypt.select(0); break;
                case USER: encrypt.select(1); break;
            }
        }
    }


    private Shell initialShell() {
        final Shell shell = new Shell(display);
        shell.setText("服务器设置");
        shell.setSize(850, 550);

        try {
            shell.setImage(new Image(display, new ImageData(ResourceManager.openIconImageStream())));
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

    private void initialComponent() {
        new ServerList();
        new ServerSettingForm();

        /*Label hostl = new Label(shell, SWT.RIGHT);
        hostl.setText("服务器");*/


        /*Button enter = new Button(shell, SWT.NONE);
        enter.setText("保存");
        enter.setBounds(400, 300, 100, 100);*/
    }


    void setVisiable(boolean visiable) {
        shell.setVisible(visiable);
    }
}
