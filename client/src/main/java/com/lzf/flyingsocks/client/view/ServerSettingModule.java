package com.lzf.flyingsocks.client.view;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.proxy.ProxyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;

class ServerSettingModule extends AbstractModule<ViewComponent> {
    private static final Logger log = LoggerFactory.getLogger(ServerSettingModule.class);

    static final String NAME = "ServerSettingModule";

    private final Frame frame;

    private DefaultListModel<String> serverListModel;

    private final Image icon;

    private ProxyServerConfig serverConfig;

    private Map<Integer, ProxyServerConfig.Node> serverNode = new LinkedHashMap<>();

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

        DefaultListModel<String> model = new DefaultListModel<>();
        this.serverListModel = model;
        JList<String> jList = new JList<>(model);
        Font font = new Font("微软雅黑", Font.BOLD, 20);
        jList.setFont(font);

        jList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        jList.setSelectedIndex(0);
        jList.setVisibleRowCount(8);

        JScrollPane jListPane = new JScrollPane(jList);
        jListPane.setBounds(10, 50, 350, 600);
        sf.add(jListPane);


        JTextField host = new JTextField();
        JLabel hostl = new JLabel("服务器IP/域名：");
        hostl.setBounds(0, 0, 100, 30);
        host.setBounds(105, 0, 335, 30);

        JTextField port = new JTextField();
        JLabel portl = new JLabel("端口(1~65535)：");
        portl.setBounds(0, 40, 100, 30);
        port.setBounds(105, 40, 335, 30);

        JTextField jksPath = new JTextField();
        JLabel jksPl = new JLabel("JKS证书路径");
        jksPl.setBounds(0, 80, 100, 30);
        jksPath.setBounds(105, 80, 335, 30);

        JPasswordField jksPass = new JPasswordField("JKS证书密码");
        JLabel jksPal = new JLabel("JKS证书密码");
        jksPal.setBounds(0, 120, 100, 30);
        jksPass.setBounds(105, 120, 335, 30);

        JComboBox<String> auth = new JComboBox<>();
        JLabel authl = new JLabel("认证方式");
        DefaultComboBoxModel<String> authModel = new DefaultComboBoxModel<>();
        authModel.addElement("选择认证方式");
        authModel.addElement("简单认证");
        authModel.addElement("用户认证");
        auth.setModel(authModel);
        auth.setSelectedIndex(0);
        authl.setBounds(0, 160, 100, 30);
        auth.setBounds(105, 160, 335, 30);

        JPasswordField simplePass = new JPasswordField();
        JLabel spl = new JLabel("密码");
        spl.setBounds(0, 200, 100, 30);
        simplePass.setBounds(105, 200, 335, 30);

        JTextField username = new JTextField();
        JLabel ul = new JLabel("用户名");
        ul.setBounds(0, 200, 100, 30);
        username.setBounds(105, 200, 335, 30);

        JPasswordField userPass = new JPasswordField();
        JLabel upl = new JLabel("用户密码");
        upl.setBounds(0, 240, 100, 30);
        userPass.setBounds(105, 240, 335, 30);


        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.add(host);
        panel.add(hostl);
        panel.add(port);
        panel.add(portl);
        panel.add(jksPath);
        panel.add(jksPal);
        panel.add(jksPass);
        panel.add(jksPl);
        panel.add(auth);
        panel.add(authl);

        auth.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED) {
                int index = auth.getSelectedIndex();
                panel.remove(username);
                panel.remove(ul);
                panel.remove(userPass);
                panel.remove(upl);
                panel.remove(simplePass);
                panel.remove(spl);
                if(index == 1) {
                    panel.add(simplePass);
                    panel.add(spl);
                } else if(index == 2) {
                    panel.add(username);
                    panel.add(userPass);
                    panel.add(ul);
                    panel.add(upl);
                }

                sf.repaint();
            }
        });

        jList.addListSelectionListener(e -> {
            ProxyServerConfig.Node n = serverNode.get(jList.getSelectedIndex());
            if(n == null)
                return;
            host.setText(n.getHost());
            port.setText("" + n.getPort());
            jksPath.setText(n.getJksPath());
            jksPass.setText(n.getJksPass());
            if(n.getAuthType() == ProxyServerConfig.AuthType.SIMPLE) {
                simplePass.setText(n.getAuthArgument("pass"));
                auth.setSelectedIndex(1);
            } else {
                username.setText(n.getAuthArgument("username"));
                userPass.setText(n.getAuthArgument("password"));
                auth.setSelectedIndex(2);
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
                    initServerNode(psc.getProxyServerConfig());
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
            serverListModel.add(i, node.getHost() + ":" + node.getPort());
            serverNode.put(i, node);
            i++;
        }
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }


}
