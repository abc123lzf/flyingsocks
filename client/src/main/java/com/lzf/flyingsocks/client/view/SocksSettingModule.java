package com.lzf.flyingsocks.client.view;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.proxy.socks.SocksConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SocksSettingModule extends AbstractModule<ViewComponent> {
    static final String NAME = "module.socks";

    private final Frame frame;
    private JComboBox<String> authTypeBox;
    private JTextField userField;
    private JPasswordField passwordField;

    SocksSettingModule(ViewComponent component, Image icon) {
        super(component, NAME);
        this.frame = initFrame(icon);
    }

    private Frame initFrame(Image icon) {
        Frame f = new Frame("Socks5代理设置");
        f.setLayout(null);
        f.setBounds(0, 0, 400, 180);
        f.setResizable(false);
        f.setIconImage(icon);

        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                f.setVisible(false);
            }
        });

        Font font = new Font("黑体", Font.BOLD, 16);

        JLabel authl = new JLabel("认证");
        authl.setFont(font);
        JComboBox<String> authBox = new JComboBox<>();
        authBox.setFont(font);
        DefaultComboBoxModel<String> authModel = new DefaultComboBoxModel<>();
        authModel.addElement("关闭");
        authModel.addElement("开启");
        authBox.setModel(authModel);

        JLabel userl = new JLabel("用户名");
        JTextField user = new JTextField();
        userl.setFont(font);
        user.setFont(font);

        JLabel pwdl = new JLabel("密码");
        JPasswordField pwd = new JPasswordField();
        pwdl.setFont(font);
        pwd.setFont(font);

        JButton save = new JButton("保存");
        JButton cancel = new JButton("取消");
        save.setFont(font);
        cancel.setFont(font);

        authl.setBounds(5, 40, 100, 30);
        authBox.setBounds(105, 40, 290, 30);

        userl.setBounds(5, 75, 100, 30);
        user.setBounds(105, 75, 290, 30);

        pwdl.setBounds(5, 110, 100, 30);
        pwd.setBounds(105, 110, 290, 30);

        save.setBounds(5, 145, 190, 30);
        cancel.setBounds(205, 145, 190, 30);

        f.add(authl);
        f.add(authBox);
        f.add(userl);
        f.add(user);
        f.add(pwdl);
        f.add(pwd);
        f.add(save);
        f.add(cancel);

        this.authTypeBox = authBox;
        this.userField = user;
        this.passwordField = pwd;

        ConfigManager<?> cm = getComponent().getParentComponent().getConfigManager();
        SocksConfig cfg = cm.getConfig(SocksConfig.NAME, SocksConfig.class);
        if(cfg == null) {
            cm.registerConfigEventListener(new ConfigEventListener() {
                @Override
                public void configEvent(ConfigEvent event) {
                    if(event.getEvent().equals(Config.REGISTER_EVENT) && event.getSource() instanceof SocksConfig) {
                        SocksConfig sc = (SocksConfig) event.getSource();
                        initConfig(sc);
                        save.addActionListener(e -> {
                            saveConfig(sc);
                        });
                        cm.removeConfigEventListener(this);
                    }
                }
            });
        } else {
            initConfig(cfg);
            save.addActionListener(e -> {
                saveConfig(cfg);
                JOptionPane.showMessageDialog(f, "成功保存", "提示", JOptionPane.INFORMATION_MESSAGE);
            });
        }

        cm.registerConfigEventListener(event -> {
            if(event.getEvent().equals(Config.UPDATE_EVENT) && event.getSource() instanceof SocksConfig) {
                initConfig((SocksConfig) event.getSource());
            }
        });

        cancel.addActionListener(e -> setVisiable(false));


        return f;
    }

    private void initConfig(SocksConfig cfg) {
        if(!cfg.isAuth())
            authTypeBox.setSelectedIndex(0);
        else
            authTypeBox.setSelectedIndex(1);

        userField.setText(cfg.getUsername());
        passwordField.setText(cfg.getPassword());
    }

    private void saveConfig(SocksConfig cfg) {
        String username = userField.getText();
        String password = new String(passwordField.getPassword());
        if((cfg.isAuth() && authTypeBox.getSelectedIndex() == 1 || !cfg.isAuth() && authTypeBox.getSelectedIndex() == 0) &&
                username.equals(cfg.getUsername()) && password.equals(cfg.getPassword()))
            return;

        cfg.update(cfg.isAuth(), username, password);
    }

    void setVisiable(boolean visiable) {
        frame.setVisible(visiable);
    }
}
