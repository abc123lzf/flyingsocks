package com.lzf.flyingsocks.client.gui.swing.serverconfigure;

import com.lzf.flyingsocks.client.gui.model.ServerAuthType;
import com.lzf.flyingsocks.client.gui.model.ServerEncryptType;
import com.lzf.flyingsocks.client.gui.model.ServerVO;
import com.lzf.flyingsocks.client.gui.swing.SwingModule;
import com.lzf.flyingsocks.client.gui.swing.SwingViewComponent;
import com.lzf.flyingsocks.client.proxy.server.ProxyServerConfig;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author lizifan 695199262@qq.com
 * @since 2021.9.25 15:49
 */
public class ServerConfigureModule extends SwingModule {

    private JFrame window;

    private ServerListUIContainer serverListContainer;

    private ServerConfigureUIContainer serverConfigureContainer;

    public ServerConfigureModule(SwingViewComponent component) {
        super(Objects.requireNonNull(component));
    }

    @Override
    protected void initial() throws Exception {
        JFrame window = createFrame(600, 300, "服务器配置");
        JSplitPane pane = new JSplitPane();

        JPanel left = new JPanel();
        this.serverListContainer = initialLeftPane(left);

        JPanel right = new JPanel();
        this.serverConfigureContainer = initialRightPane(right);
        this.serverConfigureContainer.setEditable(false);

        pane.setLeftComponent(left);
        pane.setRightComponent(right);
        pane.setResizeWeight(0.3);
        window.add(pane);

        initialMenuBar(window);
        this.window = window;


        refreshServerListContainer();
        this.serverListContainer.setServerListSelectionListener(this::onServerListItemSelected);
    }

    @Override
    public void setVisiable(boolean visiable) {
        var window = this.window;
        if (window != null) {
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            int w = window.getSize().width;
            int h = window.getSize().height;
            window.setLocation((dim.width - w) / 2, (dim.height - h) / 2);
            window.setVisible(visiable);
        }
    }


    private void refreshServerListContainer() {
        ProxyServerConfig.Node[] nodes = clientOperator.getServerNodes();
        if (nodes == null || nodes.length == 0) {
            return;
        }

        List<ServerVO> vos = new ArrayList<>();
        for (var node : nodes) {
            vos.add(new ServerVO(node));
        }
        this.serverListContainer.refresh(vos);
    }


    private void onServerListItemSelected(ServerVO vo) {
        var container = this.serverConfigureContainer;
        if (vo == null) {
            container.clean();
            container.setEditable(false);
            return;
        }
        container.setEditable(true);
        container.refresh(vo);
    }


    private void initialMenuBar(JFrame frame) {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("文件");
        JMenuItem addServerItem = new JMenuItem("添加服务器配置");
        menuBar.add(fileMenu);
        fileMenu.add(addServerItem);
        frame.setJMenuBar(menuBar);

        addServerItem.addActionListener(e -> serverListContainer.addEmpty());
    }


    private ServerListUIContainer initialLeftPane(JPanel parent) {
        parent.setLayout(new BorderLayout());
        JList<ServerVO> serverList = new JList<>();
        serverList.setBackground(new Color(62, 67, 76));
        JScrollPane panel = new JScrollPane(serverList);
        parent.add(panel, BorderLayout.CENTER);
        return new ServerListUIContainer(serverList);
    }


    private ServerConfigureUIContainer initialRightPane(JPanel parent) {
        var builder = new ServerConfigureUIContainer.Builder();

        parent.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridBagLayout());
        var defaultInset = new Insets(5, 0, 0, 10);
        {
            JLabel label = new JLabel("服务器地址", JLabel.RIGHT);
            JTextField field = new JTextField();

            var c = new GridBagConstraints();
            c.gridy = 0; c.gridx = 0;
            c.weightx = 0.2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = defaultInset;
            formPanel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 0; c.gridx = 1;
            c.weightx = 0.8;
            c.gridwidth = 3;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = defaultInset;
            formPanel.add(field, c);

            builder.hostNameField(field);
        }
        {
            JLabel label = new JLabel("端口", JLabel.RIGHT);
            JSpinner field = new JSpinner(new SpinnerNumberModel(443, 1, 65535, 1));

            var c = new GridBagConstraints();
            c.gridy = 1; c.gridx = 0;
            c.weightx = 0.2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = defaultInset;
            formPanel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 1; c.gridx = 1;
            c.weightx = 0.4;
            c.insets = defaultInset;
            c.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(field, c);

            builder.portField(field);
        }
        {
            JLabel label = new JLabel("证书端口", JLabel.RIGHT);
            JSpinner field = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));

            var c = new GridBagConstraints();
            c.gridy = 1; c.gridx = 2;
            c.weightx = 0.2;
            c.insets = defaultInset;
            c.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 1; c.gridx = 3;
            c.weightx = 0.4;
            c.insets = defaultInset;
            c.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(field, c);

            builder.certPortField(field);
        }
        {
            JLabel label = new JLabel("加密方式", JLabel.RIGHT);
            JComboBox<ServerEncryptType> comboBox = new JComboBox<>();
            for (var authType : ServerEncryptType.values()) {
                comboBox.addItem(authType);
            }

            var c = new GridBagConstraints();
            c.gridy = 2; c.gridx = 0;
            c.weightx = 0.2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = defaultInset;
            formPanel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 2; c.gridx = 1;
            c.weightx = 0.8;
            c.gridwidth = 3;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = defaultInset;
            formPanel.add(comboBox, c);

            builder.encryptTypeComboBox(comboBox);
        }
        {
            JLabel label = new JLabel("鉴权方式", JLabel.RIGHT);
            JComboBox<ServerAuthType> comboBox = new JComboBox<>();
            for (var authType : ServerAuthType.values()) {
                comboBox.addItem(authType);
            }

            var c = new GridBagConstraints();
            c.gridy = 3; c.gridx = 0;
            c.weightx = 0.2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = defaultInset;
            formPanel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 3; c.gridx = 1;
            c.weightx = 0.8;
            c.gridwidth = 3;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = defaultInset;
            formPanel.add(comboBox, c);

            builder.serverAuthTypeComboBox(comboBox);
        }
        {
            JLabel label = new JLabel("用户名", JLabel.RIGHT);
            JTextField field = new JTextField();

            var c = new GridBagConstraints();
            c.gridy = 4; c.gridx = 0;
            c.weightx = 0.2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = defaultInset;
            formPanel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 4; c.gridx = 1;
            c.gridwidth = 3;
            c.weightx = 0.8;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = defaultInset;
            formPanel.add(field, c);

            builder.userNameField(field);
        }
        {
            JLabel label = new JLabel("密码", JLabel.RIGHT);
            JPasswordField field = new JPasswordField();

            var c = new GridBagConstraints();
            c.gridy = 5; c.gridx = 0;
            c.weightx = 0.2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = defaultInset;
            formPanel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 5; c.gridx = 1;
            c.gridwidth = 3;
            c.weightx = 0.8;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = defaultInset;
            formPanel.add(field, c);

            builder.passwordField(field);
        }

        parent.add(formPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 4));
        {
            JButton save = new JButton("保存");
            JButton delete = new JButton("删除");
            save.setSize(50, 10);
            delete.setSize(50, 10);
            buttonPanel.add(new JPanel());
            buttonPanel.add(new JPanel());
            buttonPanel.add(save);
            buttonPanel.add(delete);

            save.addActionListener(e -> onServerConfigSaving());
            delete.addActionListener(e -> onServerConfigRemoving());
        }

        parent.add(buttonPanel, BorderLayout.SOUTH);
        return builder.build();
    }


    private void onServerConfigSaving() {
        var selected = serverListContainer.getSelected();
        if (selected == null) {
            showErrorMessageDialog("错误", "请选择需要保存的配置");
            return;
        }

        var hostName = serverConfigureContainer.getHostName();
        if (hostName == null || hostName.isBlank()) {
            showErrorMessageDialog("服务器配置有误", "服务器地址有误");
            JOptionPane.showMessageDialog(this.window, "服务器地址有误", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        var port = serverConfigureContainer.getPort();
        if (port == null || port <= 0 || port > 65535) {
            JOptionPane.showMessageDialog(this.window, "端口填写错误", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        var certPort = serverConfigureContainer.getCertPort();
        var authType = serverConfigureContainer.getServerAuthType();
        var encryptType = serverConfigureContainer.getEncryptType();
        var userName = serverConfigureContainer.getUserName();
        var password = serverConfigureContainer.getPassword();

        if (encryptType == ServerEncryptType.TLS_12 && certPort == null) {
            showErrorMessageDialog("服务器配置有误", "证书端口填写错误");
            return;
        }

        if (password == null || password.isEmpty()) {
            showErrorMessageDialog("服务器配置有误", "密码未填写");
            return;
        }

        if (authType == ServerAuthType.USER && (userName == null || userName.isBlank())) {
            showErrorMessageDialog("服务器配置有误", "用户名未填写");
            return;
        }

        selected.setUserName(userName);
        selected.setPort(port);
        selected.setCertPort(certPort);
        selected.setAuthType(authType);
        selected.setEncryptType(encryptType);
        selected.setUserName(userName);
        selected.setPassword(password);

        boolean isNew = selected.isNew();
        var config = selected.buildServerConfig();

        if (isNew) {
            clientOperator.addServerConfig(config);
            selected.setConfiguration(config);
        } else {
            clientOperator.updateServerConfig(config);
        }
    }

    private void onServerConfigRemoving() {
        var selected = serverListContainer.getSelected();
        if (selected == null) {
            showErrorMessageDialog("错误", "请选择需要删除的服务器");
            return;
        }

        if (!showConfirmMessageDialog("确认", "请确认是否删除该配置")) {
            return;
        }

        boolean isNew = selected.isNew();
        if (!isNew) {
            clientOperator.removeServer(selected.getConfiguration());
        }
        serverListContainer.remove(selected);
    }


    private void showErrorMessageDialog(String title, String message) {
        JOptionPane.showMessageDialog(this.window, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private boolean showConfirmMessageDialog(String title, String message) {
        int v = JOptionPane.showConfirmDialog(this.window, message, title, JOptionPane.YES_NO_OPTION);
        return v == JOptionPane.YES_OPTION;
    }
}

