package com.lzf.flyingsocks.client.gui.swing;

import com.lzf.flyingsocks.client.gui.swing.model.ServerVO;
import com.lzf.flyingsocks.client.proxy.server.ProxyServerConfig;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author lizifan 695199262@qq.com
 * @since 2021.9.25 15:49
 */
public class ServerConfigureModule extends SwingModule {

    private JFrame window;

    private ServerTreeUIContainer serverTreeContainer;

    public ServerConfigureModule(SwingViewComponent component) {
        super(Objects.requireNonNull(component));
    }

    @Override
    protected void initial() throws Exception {
        JFrame window = createFrame(600, 260, "服务器配置");
        JSplitPane pane = new JSplitPane();

        JPanel left = new JPanel();
        ServerTreeUIContainer serverTreeContainer = initialLeftPane(left);
        this.serverTreeContainer = serverTreeContainer;

        JPanel right = new JPanel();
        initialRightPane(right);

        pane.setLeftComponent(left);
        pane.setRightComponent(right);
        window.add(pane);

        this.window = window;

        refreshServerTreeContainer(serverTreeContainer);

    }

    @Override
    public void setVisiable(boolean visiable) {
        if (window != null) {
            window.setVisible(visiable);
        }
    }


    private void refreshServerTreeContainer(ServerTreeUIContainer container) {
        ProxyServerConfig.Node[] nodes = clientOperator.getServerNodes();
        DefaultMutableTreeNode serverTreeRoot = container.getServerTreeNode();

        List<DefaultMutableTreeNode> removeList = new ArrayList<>();
        // 清空无相应配置的TreeNode
        for (var it = serverTreeRoot.children(); it.hasMoreElements(); ) {
            var child = (DefaultMutableTreeNode) it.nextElement();
            var serverVO = (ServerVO) child.getUserObject();
            boolean exists = false;
            for (ProxyServerConfig.Node node : nodes) {
                if (serverVO.isSameConfiguration(node)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                removeList.add(child);
            }
        }

        removeList.forEach(serverTreeRoot::remove);

        List<DefaultMutableTreeNode> addList = new ArrayList<>();
        for (ProxyServerConfig.Node node : nodes) {
            boolean exists = false;
            for (var it = serverTreeRoot.children(); it.hasMoreElements(); ) {
                var child = (DefaultMutableTreeNode) it.nextElement();
                var serverVO = (ServerVO) child.getUserObject();
                if (serverVO.isSameConfiguration(node)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                addList.add(new DefaultMutableTreeNode(new ServerVO(node)));
            }
        }

        addList.forEach(serverTreeRoot::add);

        DefaultMutableTreeNode serverGroupTree = serverTreeContainer.getServerGroupTreeNode();

    }




    private ServerTreeUIContainer initialLeftPane(JPanel parent) {
        parent.setLayout(new BorderLayout());

        var root = new DefaultMutableTreeNode("flyingsocks");
        var server = new DefaultMutableTreeNode("服务器配置");
        var serverGroup = new DefaultMutableTreeNode("服务器组");
        root.add(server);
        root.add(serverGroup);

        JTree serverTree = new JTree(root);
        serverTree.setBackground(new Color(62, 67, 76));

        JScrollPane panel = new JScrollPane(serverTree);
        parent.add(panel, BorderLayout.CENTER);

        return new ServerTreeUIContainer(serverTree, server, serverGroup);
    }


    private void initialRightPane(JPanel parent) {
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
        }
        {
            JLabel label = new JLabel("加密方式", JLabel.RIGHT);
            JComboBox<ServerVO.ServerEncryptType> comboBox = new JComboBox<>();
            for (ServerVO.ServerEncryptType authType : ServerVO.ServerEncryptType.values()) {
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
        }
        {
            JLabel label = new JLabel("鉴权方式", JLabel.RIGHT);
            JComboBox<ServerVO.ServerAuthType> comboBox = new JComboBox<>();
            for (ServerVO.ServerAuthType authType : ServerVO.ServerAuthType.values()) {
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
        }

        parent.add(buttonPanel, BorderLayout.SOUTH);
    }
}


class ServerTreeUIContainer {

    private final DefaultMutableTreeNode serverTreeNode;

    private final DefaultMutableTreeNode serverGroupTreeNode;

    private Consumer<DefaultMutableTreeNode> serverTreeNodeSelectionListener;

    public ServerTreeUIContainer(JTree tree, DefaultMutableTreeNode serverTreeNode,
                                 DefaultMutableTreeNode serverGroupTreeNode) {
        this.serverTreeNode = serverTreeNode;
        this.serverGroupTreeNode = serverGroupTreeNode;

        tree.addTreeSelectionListener(event -> {
            if (this.serverTreeNodeSelectionListener == null) {
                return;
            }
            var node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
            serverTreeNodeSelectionListener.accept(node);
        });
    }

    public void registerServerTreeNodeSelectionListener(Consumer<DefaultMutableTreeNode> listener) {
        this.serverTreeNodeSelectionListener = listener;
    }

    public DefaultMutableTreeNode getServerGroupTreeNode() {
        return serverGroupTreeNode;
    }

    public DefaultMutableTreeNode getServerTreeNode() {
        return serverTreeNode;
    }
}


class ServerConfigureUIContainer {

    private JTextField hostNameField;

    private JSpinner portField;

    private JSpinner certPortField;

    private JComboBox<ServerVO.ServerEncryptType> encryptTypeComboBox;

    private JComboBox<ServerVO.ServerAuthType> serverAuthTypeComboBox;


}