package com.lzf.flyingsocks.client.view;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

class ServerSettingModule extends AbstractModule<ViewComponent> {
    private static final Logger log = LoggerFactory.getLogger(ServerSettingModule.class);

    static final String NAME = "ServerSettingModule";

    private final Frame frame;

    private final Image icon;

    private ProxyServerConfig serverConfig;

    //ServerList索引和配置的映射关系
    private Map<Integer, ProxyServerConfig.Node> serverNode = new LinkedHashMap<>();

    private HostList hostList;

    private SettingTable settingTable;

    ServerSettingModule(ViewComponent component, Image icon) {
        super(component, NAME);
        this.icon = icon;
        this.frame = initFrame();
        initConfig();
    }

    private Frame initFrame() {
        Frame sf = new Frame("服务器设置");
        sf.setLayout(null);

        sf.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sf.setVisible(false);
            }
        });

        sf.setIconImage(icon);
        sf.setBounds(0, 0, 800, 620);
        sf.setVisible(false);

        this.hostList = new HostList(sf, 10, 50, 300, 600);
        this.settingTable = new SettingTable(sf, 315, 50, 480, 600);

        settingTable.addEnterButtonActionListener(e -> {
            hostList.addElement("New config");
        });

        settingTable.addSaveButtonActionListener(e -> {
            int index = hostList.getSelectIndex();
            String host = settingTable.getHostField();
            int port = settingTable.getPortField();
            if(port > 65535) {
                JOptionPane.showMessageDialog(frame, "端口必须为1~65535之间的数字", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ProxyServerConfig.AuthType at = settingTable.getAuthType();
            if(at == null) {
                JOptionPane.showMessageDialog(frame, "未选择认证方式", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String[] args = settingTable.getAuthParameter();
            ProxyServerConfig.EncryptType et = settingTable.getEncryptType();

            ProxyServerConfig.Node n = updateServerNode(host, port, et, at, args);
            this.serverNode.put(index, n);
            hostList.addElement(host + ":" + port, index);
        });

        //服务器选择列表事件
        hostList.addListSelectionListener(e -> {
            ProxyServerConfig.Node n = serverNode.get(hostList.getSelectIndex());
            if(n == null) {
                settingTable.setHostField("");
                settingTable.setPortField(null);
                settingTable.setAuthInfo(ProxyServerConfig.AuthType.SIMPLE, "");
                return;
            }
            settingTable.setHostField(n.getHost());
            settingTable.setPortField(n.getPort());
            if(n.getAuthType() == ProxyServerConfig.AuthType.SIMPLE) {
                settingTable.setAuthInfo(n.getAuthType(), n.getAuthArgument("pass"));

            } else {
                settingTable.setAuthInfo(n.getAuthType(), n.getAuthArgument("username"), n.getAuthArgument("password"));
            }
        });

        return sf;
    }


    private void initConfig() {
        ConfigManager<?> manager = getComponent().getParentComponent().getConfigManager();
        ProxyServerConfig cfg = (ProxyServerConfig) manager.getConfig(ProxyServerConfig.DEFAULT_NAME);
        if(cfg == null) {
            manager.registerConfigEventListener(new ConfigEventListener() {
                @Override
                public void configEvent(ConfigEvent e) {
                    if(e.getEvent().equals(Config.REGISTER_EVENT) && e.getSource() instanceof ProxyServerConfig) {
                        ProxyServerConfig psc = (ProxyServerConfig) e.getSource();
                        ServerSettingModule.this.serverConfig = psc;
                        initServerNode(psc.getProxyServerConfig());
                        manager.removeConfigEventListener(this);
                    }
                }
            });
        } else {
            this.serverConfig = cfg;
            initServerNode(cfg.getProxyServerConfig());
        }
    }


    private void initServerNode(ProxyServerConfig.Node[] nodes) {
        int i = 0;
        for(ProxyServerConfig.Node node : nodes) {
            hostList.addElement(node.getHost() + ":" + node.getPort(), i);
            serverNode.put(i, node);
            i++;
        }
    }


    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    /**
     * 更新服务器节点
     * @param host 服务器IP/主机名
     * @param port 端口号
     * @param encryptType 加密方式
     * @param authType 认证方式
     * @param args 认证参数
     */
    private ProxyServerConfig.Node updateServerNode(String host, int port, ProxyServerConfig.EncryptType encryptType,
                                                    ProxyServerConfig.AuthType authType, String... args) {
        Map<String, String> param = new HashMap<>(4);
        switch (authType) {
            case SIMPLE:
                param.put("pass", args[0]); break;
            case USER:
                param.put("username", args[0]);
                param.put("password", args[1]);
                break;
        }
        ProxyServerConfig.Node node = new ProxyServerConfig.Node(host, port, authType, encryptType, param, false);
        this.serverConfig.addProxyServerNode(node);
        return node;
    }

    private class HostList {
        private final DefaultListModel<String> serverListModel;
        private final JList<String> serverList;

        private HostList(Frame frame, int x, int y, int width, int height) {
            serverListModel = new DefaultListModel<>();
            serverList = new JList<>(serverListModel);
            Font font = new Font("Consolas", Font.BOLD, 20);
            serverList.setFont(font);
            serverList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            serverList.setSelectedIndex(0);
            serverList.setVisibleRowCount(8);
            JScrollPane pane = new JScrollPane(serverList);
            pane.setBounds(x, y, width, height);
            frame.add(pane);
        }

        void addElement(String host) {
            serverListModel.addElement(host);
        }

        void addElement(String host, int index) {
            serverListModel.add(index, host);
        }

        int getSelectIndex() {
            return serverList.getSelectedIndex();
        }

        void addListSelectionListener(ListSelectionListener listener) {
            serverList.addListSelectionListener(listener);
        }
    }

    private class SettingTable {
        private final JTextField hostField;
        private final JTextField portField;

        private final JComboBox<String> encryptBox;
        private final DefaultComboBoxModel<String> encryptBoxModel;

        private final JComboBox<String> authBox;
        private final DefaultComboBoxModel<String> authBoxModel;

        private final JPasswordField simplePasswordField;

        private final JTextField usernameField;
        private final JPasswordField passwordField;

        private final JButton enterButton;
        private final JButton saveButton;
        private final JButton deleteButton;


        private SettingTable(Frame frame, int x, int y, int width, int height) {
            hostField = new JTextField();
            JLabel hostl = new JLabel("服务器IP / 域名");
            hostl.setBounds(0, 0, 100, 30);
            hostField.setBounds(105, 0, 335, 30);

            portField = new JTextField();
            JLabel portl = new JLabel("端口 (1~65535)");
            portl.setBounds(0, 40, 100, 30);
            portField.setBounds(105, 40, 335, 30);

            JComboBox<String> encrypt = new JComboBox<>();
            JLabel encryptl = new JLabel("加密方式");
            DefaultComboBoxModel<String> encryptModel = new DefaultComboBoxModel<>();
            encryptModel.addElement("TLS/SSL");
            encryptModel.addElement("无加密");
            encrypt.setModel(encryptModel);
            encrypt.setSelectedIndex(0);
            encryptl.setBounds(0, 80, 100, 30);
            encrypt.setBounds(105, 80, 335, 30);
            this.encryptBox = encrypt;
            this.encryptBoxModel = encryptModel;

            JComboBox<String> auth = new JComboBox<>();
            JLabel authl = new JLabel("认证方式");
            DefaultComboBoxModel<String> authModel = new DefaultComboBoxModel<>();
            authModel.addElement("选择认证方式");
            authModel.addElement("简单认证(SIMPLE)");
            authModel.addElement("用户认证(USER)");
            auth.setModel(authModel);
            auth.setSelectedIndex(0);
            authl.setBounds(0, 120, 100, 30);
            auth.setBounds(105, 120, 335, 30);
            this.authBox = auth;
            this.authBoxModel = authModel;

            JPasswordField simplePass = new JPasswordField();
            JLabel spl = new JLabel("密码");
            spl.setBounds(0, 160, 100, 30);
            simplePass.setBounds(105, 160, 335, 30);
            this.simplePasswordField = simplePass;

            JTextField username = new JTextField();
            JLabel ul = new JLabel("用户名");
            ul.setBounds(0, 160, 160, 30);
            username.setBounds(105, 160, 335, 30);
            this.usernameField = username;

            JPasswordField userPass = new JPasswordField();
            JLabel upl = new JLabel("用户密码");
            upl.setBounds(0, 200, 100, 30);
            userPass.setBounds(105, 200, 335, 30);
            this.passwordField = userPass;

            JButton enter = new JButton("新建配置");
            enter.setBounds(0, 240, 140, 30);
            JButton save = new JButton("保存");
            save.setBounds(145, 240, 140, 30);
            JButton delete = new JButton("删除");
            delete.setBounds(290, 240, 140, 30);

            this.enterButton = enter;
            this.saveButton = save;
            this.deleteButton = delete;

            JPanel panel = new JPanel();
            panel.setLayout(null);
            panel.add(hostField);
            panel.add(hostl);
            panel.add(portField);
            panel.add(portl);
            panel.add(encryptBox);
            panel.add(encryptl);
            panel.add(authBox);
            panel.add(authl);

            panel.add(enter);
            panel.add(delete);
            panel.add(save);
            panel.setBounds(x, y, width, height);
            frame.add(panel);


            auth.addItemListener(e -> {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    int index = auth.getSelectedIndex();
                    panel.remove(usernameField);
                    panel.remove(ul);
                    panel.remove(passwordField);
                    panel.remove(upl);
                    panel.remove(simplePasswordField);
                    panel.remove(spl);
                    if(index == 1) {
                        panel.add(simplePasswordField);
                        panel.add(spl);
                    } else if(index == 2) {
                        panel.add(usernameField);
                        panel.add(passwordField);
                        panel.add(ul);
                        panel.add(upl);
                    }

                    frame.repaint();
                }
            });
        }

        void setHostField(String text) {
            hostField.setText(text);
        }

        void setPortField(Integer port) {
            if(port == null)
                portField.setText("");
            else
                portField.setText(String.valueOf(port));
        }

        void setAuthInfo(ProxyServerConfig.AuthType type, String... args) {
            switch (type) {
                case SIMPLE:
                    simplePasswordField.setText(args[0]);
                    authBox.setSelectedIndex(1);
                    break;
                case USER:
                    usernameField.setText(args[0]);
                    passwordField.setText(args[1]);
                    authBox.setSelectedIndex(2);
                    break;
            }
        }

        String getHostField() {
            return hostField.getText();
        }

        int getPortField() {
            try {
                return Integer.valueOf(portField.getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "端口必须为数字", "提示", JOptionPane.WARNING_MESSAGE);
                return -1;
            }
        }

        ProxyServerConfig.EncryptType getEncryptType() {
            switch (encryptBox.getSelectedIndex()) {
                case 0:
                    return ProxyServerConfig.EncryptType.SSL;
                case 1:
                    return ProxyServerConfig.EncryptType.NONE;
                default:
                    return null;
            }
        }

        ProxyServerConfig.AuthType getAuthType() {
            switch (authBox.getSelectedIndex()) {
                case 0:
                    return null;
                case 1:
                    return ProxyServerConfig.AuthType.SIMPLE;
                case 2:
                    return ProxyServerConfig.AuthType.USER;
                default:
                    return null;
            }
        }

        String[] getAuthParameter() {
            switch (getAuthType()) {
                case SIMPLE:
                    return new String[] {usernameField.getText(), new String(passwordField.getPassword())};
                case USER:
                    return new String[] {new String(simplePasswordField.getPassword())};
                default:
                    return null;
            }
        }

        void addEnterButtonActionListener(ActionListener listener) {
            enterButton.addActionListener(listener);
        }

        void addSaveButtonActionListener(ActionListener listener) {
            saveButton.addActionListener(listener);
        }

        void addDeleteButtonActionListener(ActionListener actionListener) {
            deleteButton.addActionListener(actionListener);
        }


    }
}
