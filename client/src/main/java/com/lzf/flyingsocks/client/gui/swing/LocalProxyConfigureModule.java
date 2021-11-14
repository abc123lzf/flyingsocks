package com.lzf.flyingsocks.client.gui.swing;

import com.lzf.flyingsocks.client.gui.ResourceManager;
import com.lzf.flyingsocks.client.proxy.http.HttpProxyConfig;
import com.lzf.flyingsocks.client.proxy.socks.SocksConfig;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author lizifan 695199262@qq.com
 * @since 2021.8.22 19:28
 */
final class LocalProxyConfigureModule extends SwingModule {

    /**
     * 主窗口
     */
    private JFrame window;

    /**
     * HTTP代理设置UI容器
     */
    private HttpProxyUIContainer httpProxyUIContainer;

    /**
     * Socks代理设置UI容器
     */
    private SocksProxyUIContainer socksProxyUIContainer;

    public LocalProxyConfigureModule(SwingViewComponent component) {
        super(Objects.requireNonNull(component));
    }

    @Override
    protected void initial() throws Exception {
        // UI组件初始化
        JFrame window = createFrame(600, 255, "本地代理设置");
        window.setIconImage(ImageIO.read(ResourceManager.openIconImageStream()));

        JTabbedPane tabbedPane = new JTabbedPane();
        Pair<JPanel, HttpProxyUIContainer> httpPane = initialHttpProxySettingPanel();
        this.httpProxyUIContainer = httpPane.getRight();
        tabbedPane.add(" HTTP代理 ", httpPane.getLeft());

        Pair<JPanel, SocksProxyUIContainer> socksPane = initialSocksProxySettingPanel();
        this.socksProxyUIContainer = socksPane.getRight();
        tabbedPane.add(" SOCKS代理 ", socksPane.getLeft());
        window.add(tabbedPane);

        this.window = window;

        refreshHttpProxyContent();
        refreshSocksProxyContent();

        // UI交互初始化
        this.httpProxyUIContainer.registerSaveButtonPressedListener(this::httpProxyContainerSaveButtonAction);
        this.httpProxyUIContainer.registerCancelButtonPressedListener(e -> {
            refreshHttpProxyContent();
            setVisiable(false);
        });
        this.socksProxyUIContainer.registerSaveButtonPressedListener(this::socksProxyContainerSaveButtonAction);
        this.socksProxyUIContainer.registerSaveButtonPressedListener(e -> {
            refreshSocksProxyContent();
            setVisiable(false);
        });
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                refreshHttpProxyContent();
                refreshSocksProxyContent();
            }
        });
    }

    @Override
    public void setVisiable(boolean visiable) {
        JFrame window = this.window;
        if (window != null) {
            window.setVisible(visiable);
        }
    }


    private void refreshHttpProxyContent() {
        HttpProxyUIContainer container = this.httpProxyUIContainer;
        HttpProxyConfig config = clientOperator.getHttpProxyConfig();
        if (config == null) {
            container.setOpen(false);
            container.setAuthOpen(false);
            container.setPort(8080);
            container.setUserName("");
            container.setPassword("");
            return;
        }
        container.setOpen(true);
        container.setAuthOpen(config.isAuth());
        container.setPort(config.getBindPort());
        container.setUserName(config.getUsername());
        container.setPassword(config.getPassword());
    }


    private void refreshSocksProxyContent() {
        SocksProxyUIContainer container = this.socksProxyUIContainer;
        SocksConfig config = clientOperator.getSocksConfig();
        if (config == null) {
            container.setOpen(false);
            container.setPort(1080);
            container.setAuthOpen(false);
            container.setUserName("");
            container.setPassword("");
            return;
        }
        container.setOpen(true);
        container.setPort(config.getPort());
        container.setAuthOpen(config.isAuth());
        container.setUserName(config.getUsername());
        container.setPassword(config.getPassword());
    }


    private void httpProxyContainerSaveButtonAction(HttpProxyUIContainer container) {
        boolean open = container.isOpen();
        int port = container.readPort();
        boolean auth = container.isAuthOpen();
        String userName = container.readUserName();
        String password = container.readPassword();
        clientOperator.updateHttpProxyConfig(open, port, auth, userName, password);
        belongComponent.asyncInvoke(this::refreshHttpProxyContent);
    }


    private void socksProxyContainerSaveButtonAction(SocksProxyUIContainer container) {
        boolean open = container.isOpen();
        int port = container.readPort();
        boolean auth = container.isAuthOpen();
        String userName = container.readUserName();
        String password = container.readPassword();
        clientOperator.updateSocksProxyAuthentication(port, auth, userName, password);
        belongComponent.asyncInvoke(this::refreshSocksProxyContent);
    }


    private Pair<JPanel, HttpProxyUIContainer> initialHttpProxySettingPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        var builder = new HttpProxyUIContainer.Builder();
        {
            JLabel label = new JLabel("开关", JLabel.RIGHT);
            JRadioButton open = new JRadioButton("开启");
            JRadioButton close = new JRadioButton("关闭");
            bindRadioButtonGroup(open, close);
            builder.openButton(open).closeButton(close);

            var c = new GridBagConstraints();
            c.gridy = 0; c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 0.1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(5, 0, 0, 20);
            panel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 0; c.gridx = 1;
            c.insets = new Insets(5, 0, 0, 5);
            c.gridwidth = 1;
            panel.add(open, c);
            c.gridx = 2;
            panel.add(close, c);
        }
        {
            JLabel label = new JLabel("端口", JLabel.RIGHT);
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(1080, 1, 65535, 1));
            builder.portField(spinner);

            var c = new GridBagConstraints();
            c.gridy = 1; c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 0.1;
            c.insets = new Insets(5, 0, 0, 20);
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 1; c.gridx = 1;
            c.gridheight = 1; c.gridwidth = 3;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.LINE_START;
            c.insets = new Insets(5, 0, 0, 10);
            panel.add(spinner, c);
        }
        {
            JLabel label = new JLabel("鉴权", JLabel.RIGHT);
            JRadioButton open = new JRadioButton("开启");
            JRadioButton close = new JRadioButton("关闭");
            bindRadioButtonGroup(open, close);
            builder.authOpenButton(open).authCloseButton(close);

            var c = new GridBagConstraints();
            c.gridy = 2; c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 0.1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(5, 0, 0, 20);
            panel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 2; c.gridx = 1;
            c.gridwidth = 1;
            c.insets = new Insets(5, 0, 0, 5);
            panel.add(open, c);
            c.gridx = 2;
            panel.add(close, c);
        }
        {
            JLabel label = new JLabel("用户名", JLabel.RIGHT);
            JTextField field = new JTextField();
            builder.userNameField(field);

            var c = new GridBagConstraints();
            c.gridy = 3; c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 0.1;
            c.insets = new Insets(5, 0, 0, 20);
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 3; c.gridx = 1;
            c.gridwidth = 5;
            c.weightx = 0.7;
            c.insets = new Insets(5, 0, 0, 10);
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(field, c);
        }
        {
            JLabel label = new JLabel("密码", JLabel.RIGHT);
            JPasswordField field = new JPasswordField();
            builder.passwordField(field);

            var c = new GridBagConstraints();
            c.gridy = 4; c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 0.1;
            c.insets = new Insets(5, 0, 0, 20);
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 4; c.gridx = 1;
            c.gridwidth = 5;
            c.weightx = 0.7;
            c.insets = new Insets(5, 0, 0, 10);
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(field, c);
        }
        {
            JButton enter = new JButton("保存");
            JButton cancel = new JButton("取消");
            builder.saveButton(enter).cancelButton(cancel);

            var c = new GridBagConstraints();
            c.gridy = 5; c.gridx = 4;
            c.weightx = 0.2; c.weighty = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.PAGE_END;
            c.ipadx = 5;
            c.insets = new Insets(8, 0, 0, 5);
            panel.add(enter, c);

            c = new GridBagConstraints();
            c.gridy = 5; c.gridx = 5;
            c.weightx = 0.2; c.weighty = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.PAGE_END;
            c.ipadx = 5;
            c.insets = new Insets(8, 0, 0, 5);
            panel.add(cancel, c);
        }

        return Pair.of(panel, builder.build());
    }


    private Pair<JPanel, SocksProxyUIContainer> initialSocksProxySettingPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        var builder = new SocksProxyUIContainer.Builder();
        {
            JLabel label = new JLabel("开关", JLabel.RIGHT);
            JRadioButton open = new JRadioButton("开启");
            JRadioButton close = new JRadioButton("关闭");
            bindRadioButtonGroup(open, close);
            builder.openButton(open).closeButton(close);

            var c = new GridBagConstraints();
            c.gridy = 0; c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 0.1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(5, 0, 0, 20);
            panel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 0; c.gridx = 1;
            c.insets = new Insets(5, 0, 0, 5);
            c.gridwidth = 1;
            panel.add(open, c);
            c.gridx = 2;
            panel.add(close, c);
        }
        {
            JLabel label = new JLabel("端口", JLabel.RIGHT);
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(8080, 1, 65535, 1));
            builder.portField(spinner);

            var c = new GridBagConstraints();
            c.gridy = 1; c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 0.1;
            c.insets = new Insets(5, 0, 0, 20);
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 1; c.gridx = 1;
            c.gridheight = 1; c.gridwidth = 3;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.LINE_START;
            c.insets = new Insets(5, 0, 0, 10);
            panel.add(spinner, c);
        }
        {
            JLabel label = new JLabel("鉴权", JLabel.RIGHT);
            JRadioButton open = new JRadioButton("开启");
            JRadioButton close = new JRadioButton("关闭");
            bindRadioButtonGroup(open, close);
            builder.authOpenButton(open).authCloseButton(close);

            var c = new GridBagConstraints();
            c.gridy = 2; c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 0.1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(5, 0, 0, 20);
            panel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 2; c.gridx = 1;
            c.gridwidth = 1;
            c.insets = new Insets(5, 0, 0, 5);
            panel.add(open, c);
            c.gridx = 2;
            panel.add(close, c);
        }
        {
            JLabel label = new JLabel("用户名", JLabel.RIGHT);
            JTextField field = new JTextField();
            builder.userNameField(field);

            var c = new GridBagConstraints();
            c.gridy = 3; c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 0.1;
            c.insets = new Insets(5, 0, 0, 20);
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 3; c.gridx = 1;
            c.gridwidth = 5;
            c.weightx = 0.7;
            c.insets = new Insets(5, 0, 0, 10);
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(field, c);
        }
        {
            JLabel label = new JLabel("密码", JLabel.RIGHT);
            JPasswordField field = new JPasswordField();
            builder.passwordField(field);

            var c = new GridBagConstraints();
            c.gridy = 4; c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 0.1;
            c.insets = new Insets(5, 0, 0, 20);
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(label, c);

            c = new GridBagConstraints();
            c.gridy = 4; c.gridx = 1;
            c.gridwidth = 5;
            c.weightx = 0.7;
            c.insets = new Insets(5, 0, 0, 10);
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(field, c);
        }
        {
            JButton enter = new JButton("保存");
            JButton cancel = new JButton("取消");
            builder.saveButton(enter).cancelButton(cancel);

            var c = new GridBagConstraints();
            c.gridy = 5; c.gridx = 4;
            c.weightx = 0.1; c.weighty = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.PAGE_END;
            c.ipadx = 5;
            c.insets = new Insets(8, 0, 0, 5);
            panel.add(enter, c);

            c = new GridBagConstraints();
            c.gridy = 5; c.gridx = 5;
            c.weightx = 0.1; c.weighty = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.PAGE_END;
            c.ipadx = 5;
            c.insets = new Insets(8, 0, 0, 5);
            panel.add(cancel, c);
        }

        return Pair.of(panel, builder.build());
    }
}


class HttpProxyUIContainer {

    private final JRadioButton openButton, closeButton;

    private final JSpinner portField;

    private final JRadioButton authOpenButton, authCloseButton;

    private final JTextField userNameField;

    private final JPasswordField passwordField;

    private Consumer<HttpProxyUIContainer> saveButtonPressedListener;

    private Consumer<HttpProxyUIContainer> cancelButtonPressedListener;

    HttpProxyUIContainer(JRadioButton openButton, JRadioButton closeButton, JSpinner portField,
                                JRadioButton authOpenButton, JRadioButton authCloseButton, JTextField userNameField,
                                JPasswordField passwordField, JButton saveButton, JButton cancelButton) {
        this.openButton = Objects.requireNonNull(openButton);
        this.closeButton = Objects.requireNonNull(closeButton);
        this.portField = Objects.requireNonNull(portField);
        this.authOpenButton = Objects.requireNonNull(authOpenButton);
        this.authCloseButton = Objects.requireNonNull(authCloseButton);
        this.userNameField = Objects.requireNonNull(userNameField);
        this.passwordField = Objects.requireNonNull(passwordField);
        saveButton.addActionListener(e -> {
            if (saveButtonPressedListener != null) {
                saveButtonPressedListener.accept(this);
            }
        });
        cancelButton.addActionListener(e -> {
            if (cancelButtonPressedListener != null) {
                cancelButtonPressedListener.accept(this);
            }
        });
    }

    boolean isOpen() {
        return openButton.isSelected();
    }

    void setOpen(boolean open) {
        if (open) {
            openButton.setSelected(true);
        } else {
            closeButton.setSelected(true);
        }
    }

    int readPort() {
        return Integer.parseInt(portField.getValue().toString());
    }

    void setPort(int port) {
        portField.setValue(port);
    }

    boolean isAuthOpen() {
        return authOpenButton.isSelected();
    }

    void setAuthOpen(boolean open) {
        if (open) {
            authOpenButton.setSelected(true);
        } else {
            authCloseButton.setSelected(true);
        }
    }

    String readUserName() {
        return userNameField.getText();
    }

    void setUserName(String userName) {
        userNameField.setText(userName);
    }

    String readPassword() {
        return new String(passwordField.getPassword());
    }

    void setPassword(String password) {
        passwordField.setText(password);
    }

    void registerSaveButtonPressedListener(Consumer<HttpProxyUIContainer> saveButtonPressedListener) {
        this.saveButtonPressedListener = saveButtonPressedListener;
    }

    void registerCancelButtonPressedListener(Consumer<HttpProxyUIContainer> cancelButtonPressedListener) {
        this.cancelButtonPressedListener = cancelButtonPressedListener;
    }

    static class Builder {
        private JRadioButton openButton, closeButton;
        private JSpinner portField;
        private JRadioButton authOpenButton, authCloseButton;
        private JTextField userNameField;
        private JPasswordField passwordField;
        private JButton saveButton, cancelButton;
        Builder openButton(JRadioButton openButton) {
            this.openButton = openButton;
            return this;
        }
        Builder closeButton(JRadioButton closeButton) {
            this.closeButton = closeButton;
            return this;
        }
        Builder portField(JSpinner portField) {
            this.portField = portField;
            return this;
        }
        Builder authOpenButton(JRadioButton authOpenButton) {
            this.authOpenButton = authOpenButton;
            return this;
        }
        Builder authCloseButton(JRadioButton authCloseButton) {
            this.authCloseButton = authCloseButton;
            return this;
        }
        Builder userNameField(JTextField userNameField) {
            this.userNameField = userNameField;
            return this;
        }
        Builder passwordField(JPasswordField passwordField) {
            this.passwordField = passwordField;
            return this;
        }
        Builder saveButton(JButton saveButton) {
            this.saveButton = saveButton;
            return this;
        }
        Builder cancelButton(JButton cancelButton) {
            this.cancelButton = cancelButton;
            return this;
        }
        HttpProxyUIContainer build() {
            return new HttpProxyUIContainer(openButton, closeButton, portField, authOpenButton, authCloseButton,
                    userNameField, passwordField, saveButton, cancelButton);
        }
    }
}

class SocksProxyUIContainer {

    private final JRadioButton openButton, closeButton;

    private final JSpinner portField;

    private final JRadioButton authOpenButton, authCloseButton;

    private final JTextField userNameField;

    private final JPasswordField passwordField;

    private Consumer<SocksProxyUIContainer> saveButtonPressedListener;

    private Consumer<SocksProxyUIContainer> cancelButtonPressedListener;

    SocksProxyUIContainer(JRadioButton openButton, JRadioButton closeButton, JSpinner portField,
                         JRadioButton authOpenButton, JRadioButton authCloseButton, JTextField userNameField,
                         JPasswordField passwordField, JButton saveButton, JButton cancelButton) {
        this.openButton = Objects.requireNonNull(openButton);
        this.closeButton = Objects.requireNonNull(closeButton);
        this.portField = Objects.requireNonNull(portField);
        this.authOpenButton = Objects.requireNonNull(authOpenButton);
        this.authCloseButton = Objects.requireNonNull(authCloseButton);
        this.userNameField = Objects.requireNonNull(userNameField);
        this.passwordField = Objects.requireNonNull(passwordField);
        saveButton.addActionListener(e -> {
            if (saveButtonPressedListener != null) {
                saveButtonPressedListener.accept(this);
            }
        });
        cancelButton.addActionListener(e -> {
            if (cancelButtonPressedListener != null) {
                cancelButtonPressedListener.accept(this);
            }
        });
    }

    boolean isOpen() {
        return openButton.isSelected();
    }

    void setOpen(boolean open) {
        if (open) {
            openButton.setSelected(true);
        } else {
            closeButton.setSelected(true);
        }
    }

    int readPort() {
        return Integer.parseInt(portField.getValue().toString());
    }

    void setPort(int port) {
        portField.setValue(port);
    }

    boolean isAuthOpen() {
        return authOpenButton.isSelected();
    }

    void setAuthOpen(boolean open) {
        if (open) {
            authOpenButton.setSelected(true);
        } else {
            authCloseButton.setSelected(true);
        }
    }

    String readUserName() {
        return userNameField.getText();
    }

    void setUserName(String userName) {
        userNameField.setText(userName);
    }

    String readPassword() {
        return new String(passwordField.getPassword());
    }

    void setPassword(String password) {
        passwordField.setText(password);
    }

    void registerSaveButtonPressedListener(Consumer<SocksProxyUIContainer> saveButtonPressedListener) {
        this.saveButtonPressedListener = saveButtonPressedListener;
    }

    void registerCancelButtonPressedListener(Consumer<SocksProxyUIContainer> listener) {
        this.cancelButtonPressedListener = listener;
    }

    static class Builder {
        private JRadioButton openButton, closeButton;
        private JSpinner portField;
        private JRadioButton authOpenButton, authCloseButton;
        private JTextField userNameField;
        private JPasswordField passwordField;
        private JButton saveButton, cancelButton;
        Builder openButton(JRadioButton openButton) {
            this.openButton = openButton;
            return this;
        }
        Builder closeButton(JRadioButton closeButton) {
            this.closeButton = closeButton;
            return this;
        }
        Builder portField(JSpinner portField) {
            this.portField = portField;
            return this;
        }
        Builder authOpenButton(JRadioButton authOpenButton) {
            this.authOpenButton = authOpenButton;
            return this;
        }
        Builder authCloseButton(JRadioButton authCloseButton) {
            this.authCloseButton = authCloseButton;
            return this;
        }
        Builder userNameField(JTextField userNameField) {
            this.userNameField = userNameField;
            return this;
        }
        Builder passwordField(JPasswordField passwordField) {
            this.passwordField = passwordField;
            return this;
        }
        Builder saveButton(JButton saveButton) {
            this.saveButton = saveButton;
            return this;
        }
        Builder cancelButton(JButton cancelButton) {
            this.cancelButton = cancelButton;
            return this;
        }
        SocksProxyUIContainer build() {
            return new SocksProxyUIContainer(openButton, closeButton, portField, authOpenButton, authCloseButton,
                    userNameField, passwordField, saveButton, cancelButton);
        }
    }
}