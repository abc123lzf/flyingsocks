package com.lzf.flyingsocks.client.gui.swing;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.Client;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ServerSettingModule extends AbstractModule<SwingViewComponent> {

    private static final Logger log = LoggerFactory.getLogger(ServerSettingModule.class);

    /**
     * 添加服务器配置时GUI左侧选项名称
     */
    private static final String NEW_CONFIG_NAME = "New config";

    /**
     * 模块名
     */
    static final String NAME = "ServerSettingModule";

    /**
     * 客户端
     */
    private final Client client;

    /**
     * 窗口对象
     */
    private final Frame frame;

    /**
     * ServerList索引和配置的映射关系，GUI界面为单线程无需保证线程安全问题
     */
    private final List<ProxyServerConfig.Node> serverNode = new ArrayList<>();

    /**
     * GUI界面服务器列表
     */
    private HostList hostList;

    /**
     * 服务器编辑表单
     */
    private SettingTable settingTable;

    /**
     * @param component 父组件
     * @param icon 界面左上角图片
     */
    ServerSettingModule(SwingViewComponent component, Image icon) {
        super(component, NAME);
        this.client = component.getParentComponent();
        this.frame = initFrame(icon);
        initConfig();
    }

    private Frame initFrame(Image icon) {
        Frame sf = new Frame("服务器设置");
        sf.setLayout(null);

        sf.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sf.setVisible(false);
            }
        });

        sf.setIconImage(icon);
        sf.setBounds(0, 0, 760, 335);
        sf.setResizable(false);
        sf.setLocationRelativeTo(null);
        sf.setVisible(false);

        this.hostList = new HostList(sf, 10, 50, 300, 278);
        this.settingTable = new SettingTable(sf, 315, 50, 480, 275);

        settingTable.addEnterButtonActionListener(e -> {
            hostList.addElement(NEW_CONFIG_NAME);
        });

        settingTable.addSaveButtonActionListener(e -> {
            int index = hostList.getSelectIndex();
            if(index == -1) {
                JOptionPane.showMessageDialog(frame, "请首先点击新建配置再保存", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String host = settingTable.getHostField();
            int port = settingTable.getPortField();
            if(port < 0)
                return;
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

            int certPort = -1;
            if(et == ProxyServerConfig.EncryptType.SSL) {
                certPort = settingTable.getCertPort();
            }

            if(hostList.getSelectContent().equals(NEW_CONFIG_NAME)) {
                ProxyServerConfig.Node n = addServerNode(host, port, certPort, et, at, args);
                this.serverNode.add(n);
            } else {
                updateServerNode(this.serverNode.get(index), host, port, certPort, et, at, args);
            }
            hostList.removeElement(index);
            hostList.addElement(host + ":" + port, index);
            JOptionPane.showMessageDialog(frame, "成功保存配置，请右键托盘图标点击‘选择服务器’进行连接");
        });

        settingTable.addDeleteButtonActionListener(e -> {
            int index = hostList.getSelectIndex();
            if(hostList.getSelectContent().equals(NEW_CONFIG_NAME)) {
                hostList.removeElement(index);
                return;
            }
            hostList.removeElement(index);
            removeServerNode(serverNode.remove(index));
        });

        //服务器选择列表事件
        hostList.addListSelectionListener(e -> {
            int index = hostList.getSelectIndex();
            if(index == -1 || index >= serverNode.size()) {
                settingTable.setHostField("");
                settingTable.setPortField(null);
                settingTable.setAuthInfo(ProxyServerConfig.AuthType.SIMPLE, "");
                return;
            }
            ProxyServerConfig.Node n = serverNode.get(index);
            if(n == null) {
                settingTable.setHostField("");
                settingTable.setPortField(null);
                settingTable.setAuthInfo(ProxyServerConfig.AuthType.SIMPLE, "");
                return;
            }
            settingTable.setHostField(n.getHost());
            settingTable.setPortField(n.getPort());
            if(n.getAuthType() == ProxyServerConfig.AuthType.SIMPLE) {
                settingTable.setAuthInfo(n.getAuthType(), n.getAuthArgument("password"));
            } else {
                settingTable.setAuthInfo(n.getAuthType(), n.getAuthArgument("user"), n.getAuthArgument("pass"));
            }
        });

        return sf;
    }


    private void initConfig() {
        ProxyServerConfig.Node[] nodes = client.getServerNodes();
        if(nodes == null) {
            ConfigManager<?> manager = belongComponent.getParentComponent().getConfigManager();
            manager.registerConfigEventListener(new ConfigEventListener() {
                @Override
                public void configEvent(ConfigEvent e) {
                    if(e.getEvent().equals(Config.REGISTER_EVENT) && e.getSource() instanceof ProxyServerConfig) {
                        ProxyServerConfig psc = (ProxyServerConfig) e.getSource();
                        initServerNode(psc.getProxyServerConfig());
                        manager.removeConfigEventListener(this);
                    }
                }
            });
        } else {
            initServerNode(nodes);
        }
    }


    private void initServerNode(ProxyServerConfig.Node[] nodes) {
        int i = 0;
        for(ProxyServerConfig.Node node : nodes) {
            hostList.addElement(node.getHost() + ":" + node.getPort(), i);
            serverNode.add(i, node);
            i++;
        }
    }


    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    /**
     * 更新服务器节点
     * @param host 服务器IP/主机名
     * @param port 代理连接端口
     * @param certPort 用于获取证书的端口
     * @param encryptType 加密方式
     * @param authType 认证方式
     * @param args 认证参数
     */
    private ProxyServerConfig.Node addServerNode(String host, int port, int certPort, ProxyServerConfig.EncryptType encryptType,
                                                 ProxyServerConfig.AuthType authType, String... args) {
        Map<String, String> param = new HashMap<>(4, 1);
        switch (authType) {
            case SIMPLE:
                param.put("password", args[0]); break;
            case USER:
                param.put("user", args[0]);
                param.put("pass", args[1]);
                break;
        }
        ProxyServerConfig.Node node = new ProxyServerConfig.Node(host, port, certPort, authType, encryptType, param, false);

        client.addServerConfig(node);
        return node;
    }


    private void updateServerNode(ProxyServerConfig.Node src, String host, int port, int certPort,
                                                    ProxyServerConfig.EncryptType encryptType,
                                                    ProxyServerConfig.AuthType authType, String... args) {
        Map<String, String> param = new HashMap<>(4);
        switch (authType) {
            case SIMPLE:
                param.put("password", args[0]); break;
            case USER:
                param.put("user", args[0]);
                param.put("pass", args[1]);
                break;
        }

        if(encryptType == ProxyServerConfig.EncryptType.SSL)
            src.setCertPort(certPort);

        src.setHost(host);
        src.setPort(port);
        src.setCertPort(certPort);
        src.setEncryptType(encryptType);
        src.setAuthType(authType);
        src.setAuthArgument(param);

        client.updateServerConfig(src);
    }


    private void removeServerNode(ProxyServerConfig.Node node) {
        if(node == null) {
            log.warn("Node is null");
            return;
        }

        client.removeServer(node);
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
            if(host.equals(NEW_CONFIG_NAME))
                serverList.setSelectedIndex(serverListModel.size() - 1);
        }

        void addElement(String host, int index) {
            serverListModel.add(index, host);
        }

        void removeElement(String host) {
            serverListModel.removeElement(host);
        }

        void removeElement(int index) {
            serverListModel.remove(index);
        }

        int getSelectIndex() {
            return serverList.getSelectedIndex();
        }

        String getSelectContent() {
            return serverList.getSelectedValue();
        }

        void addListSelectionListener(ListSelectionListener listener) {
            serverList.addListSelectionListener(listener);
        }
    }

    private class SettingTable {
        //主机名文本框
        private final JTextField hostField;

        //端口号文本框
        private final JTextField portField;

        //加密方式下拉框
        private final JComboBox<String> encryptBox;
        private final DefaultComboBoxModel<String> encryptBoxModel;

        //证书获取端口文本框(仅OpenSSL)
        private final JTextField certPortField;

        //认证方式下拉框
        private final JComboBox<String> authBox;
        private final DefaultComboBoxModel<String> authBoxModel;

        //认证方式为SIMPLE时的密码框
        private final JPasswordField simplePasswordField;

        //认证方式为USER时的用户、密码框
        private final JTextField usernameField;
        private final JPasswordField passwordField;

        //确认按钮
        private final JButton enterButton;
        //保存按钮
        private final JButton saveButton;
        //删除按钮
        private final JButton deleteButton;

        private SettingTable(Frame frame, int x, int y, int width, int height) {
            Font font = new Font("黑体", Font.BOLD, 16);

            hostField = new JTextField();
            hostField.setFont(font);
            JLabel hostl = new JLabel("服务器地址");
            hostl.setFont(font);
            hostl.setBounds(0, 0, 100, 30);
            hostField.setBounds(105, 0, 335, 30);

            portField = new JTextField();
            portField.setFont(font);
            JLabel portl = new JLabel("端口");
            portl.setFont(font);
            portl.setBounds(0, 40, 100, 30);
            portField.setBounds(105, 40, 120, 30);

            certPortField = new JTextField();
            certPortField.setFont(font);
            JLabel cpl = new JLabel(" 证书端口");
            cpl.setFont(font);
            cpl.setBounds(225, 40, 100, 30);
            certPortField.setBounds(330, 40, 110, 30);

            JComboBox<String> encrypt = new JComboBox<>();
            JLabel encryptl = new JLabel("加密方式");
            encryptl.setFont(font);
            DefaultComboBoxModel<String> encryptModel = new DefaultComboBoxModel<>();
            encryptModel.addElement("TLS/SSL");
            encryptModel.addElement("无加密");
            encrypt.setModel(encryptModel);
            encrypt.setSelectedIndex(0);
            encryptl.setBounds(0, 80, 100, 30);
            encrypt.setBounds(105, 80, 335, 30);
            encrypt.setFont(font);
            this.encryptBox = encrypt;
            this.encryptBoxModel = encryptModel;

            JComboBox<String> auth = new JComboBox<>();
            JLabel authl = new JLabel("认证方式");
            authl.setFont(font);
            DefaultComboBoxModel<String> authModel = new DefaultComboBoxModel<>();
            authModel.addElement("选择认证方式");
            authModel.addElement("简单认证(SIMPLE)");
            authModel.addElement("用户认证(USER)");
            auth.setModel(authModel);
            auth.setSelectedIndex(0);
            authl.setBounds(0, 120, 100, 30);
            auth.setBounds(105, 120, 335, 30);
            auth.setFont(font);
            this.authBox = auth;
            this.authBoxModel = authModel;

            JPasswordField simplePass = new JPasswordField();
            JLabel spl = new JLabel("密码");
            spl.setFont(font);
            spl.setBounds(0, 160, 100, 30);
            simplePass.setBounds(105, 160, 335, 30);
            this.simplePasswordField = simplePass;

            JTextField username = new JTextField();
            JLabel ul = new JLabel("用户名");
            ul.setFont(font);
            username.setFont(font);
            ul.setBounds(0, 160, 160, 30);
            username.setBounds(105, 160, 335, 30);
            this.usernameField = username;

            JPasswordField userPass = new JPasswordField();
            JLabel upl = new JLabel("用户密码");
            upl.setFont(font);
            upl.setBounds(0, 200, 100, 30);
            userPass.setBounds(105, 200, 335, 30);
            this.passwordField = userPass;

            JButton enter = new JButton("新建配置");
            enter.setFont(font);
            enter.setBounds(0, 240, 140, 30);
            JButton save = new JButton("保存");
            save.setFont(font);
            save.setBounds(145, 240, 140, 30);
            JButton delete = new JButton("删除");
            delete.setFont(font);
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
            panel.add(certPortField);
            panel.add(cpl);
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

                    panel.repaint();
                }
            });

            encrypt.addItemListener(e -> {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    if(encrypt.getSelectedIndex() == 0) {
                        certPortField.setEditable(true);
                    } else {
                        certPortField.setEditable(false);
                    }
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

        int getCertPort() {
            try {
                return Integer.valueOf(certPortField.getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "证书端口必须为数字", "提示", JOptionPane.WARNING_MESSAGE);
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
                case USER:
                    return new String[] {usernameField.getText(), new String(passwordField.getPassword())};
                case SIMPLE:
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
