package com.lzf.flyingsocks.client.view;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class ServerSettingModule extends AbstractModule<ViewComponent> {
    static final String NAME = "ServerSettingModule";

    private final Frame frame;

    private DefaultListModel<String> serverListModel;

    private final Image icon;

    private ProxyServerConfig serverConfig;

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
        sf.setBounds(0, 0, 800, 600);
        sf.setVisible(false);

        DefaultListModel<String> model = new DefaultListModel<>();
        this.serverListModel = model;
        JList<String> jList = new JList<>(model);

        jList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        jList.setSelectedIndex(0);
        jList.setVisibleRowCount(8);

        JScrollPane jListPane = new JScrollPane(jList);
        jListPane.setBounds(10, 50, 350, 600);
        sf.add(jListPane);

        JTextField host = new JTextField("服务器IP/域名");
        JTextField port = new JTextField("端口(1~65535)");
        JTextField jksPath = new JTextField("JKS证书路径");
        JPasswordField jksPass = new JPasswordField("JKS证书密码");

        JComboBox<String> auth = new JComboBox<>();
        DefaultComboBoxModel<String> authModel = new DefaultComboBoxModel<>();
        authModel.addElement("选择认证方式");
        authModel.addElement("简单认证");
        authModel.addElement("用户认证");
        auth.setModel(authModel);

        auth.setSelectedIndex(0);

        JPasswordField simplePass = new JPasswordField("密码");

        JTextField username = new JTextField("用户名");
        JPasswordField userPass = new JPasswordField("用户密码");

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(host);
        panel.add(port);
        panel.add(jksPath);
        panel.add(jksPass);
        panel.add(auth);

        auth.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED) {
                int index = auth.getSelectedIndex();
                panel.remove(username);
                panel.remove(userPass);
                panel.remove(simplePass);
                if(index == 1) {
                    panel.add(simplePass);
                } else if(index == 2) {
                    panel.add(username);
                    panel.add(userPass);
                }

                sf.repaint();
            }
        });


        panel.setBounds(365, 50, 445, 600);
        sf.add(panel);

        return sf;
    }

    private void initConfig() {
        ConfigManager<?> manager = getComponent().getParentComponent().getConfigManager();
        ProxyServerConfig cfg = (ProxyServerConfig) manager.getConfig(ProxyServerConfig.DEFAULT_NAME);
        if(cfg == null) {
            manager.registerConfigEventListener(e -> {
                if(e.getEvent().equals(Config.REGISTER_EVENT) && e.getSource() instanceof ProxyServerConfig) {
                    ProxyServerConfig psc = (ProxyServerConfig) e.getSource();
                    this.serverConfig = psc;
                    ProxyServerConfig.Node[] nodes = psc.getProxyServerConfig();
                    for(ProxyServerConfig.Node node : nodes) {
                        serverListModel.addElement(node.getHost());
                    }
                }
            });
        } else {
            this.serverConfig = cfg;
            ProxyServerConfig.Node[] nodes = cfg.getProxyServerConfig();
            for(ProxyServerConfig.Node node : nodes) {
                serverListModel.addElement(node.getHost() + ":" + node.getPort());
            }
        }


    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }


}
