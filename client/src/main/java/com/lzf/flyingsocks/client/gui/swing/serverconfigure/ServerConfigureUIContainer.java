package com.lzf.flyingsocks.client.gui.swing.serverconfigure;

import com.lzf.flyingsocks.client.gui.model.ServerAuthType;
import com.lzf.flyingsocks.client.gui.model.ServerEncryptType;
import com.lzf.flyingsocks.client.gui.model.ServerVO;

import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.util.Objects;

/**
 * @author lizifan
 * @since 2022.1.2 18:51
 */
class ServerConfigureUIContainer {

    private final JTextField hostNameField;

    private final JSpinner portField;

    private final JSpinner certPortField;

    private final JComboBox<ServerEncryptType> encryptTypeComboBox;

    private final JComboBox<ServerAuthType> serverAuthTypeComboBox;

    private final JTextField userNameField;

    private final JPasswordField passwordField;

    public ServerConfigureUIContainer(JTextField hostNameField, JSpinner portField, JSpinner certPortField,
                                      JComboBox<ServerEncryptType> encryptTypeComboBox,
                                      JComboBox<ServerAuthType> serverAuthTypeComboBox,
                                      JTextField userNameField, JPasswordField passwordField) {
        this.hostNameField = Objects.requireNonNull(hostNameField);
        this.portField = Objects.requireNonNull(portField);
        this.certPortField = Objects.requireNonNull(certPortField);
        this.encryptTypeComboBox = Objects.requireNonNull(encryptTypeComboBox);
        this.serverAuthTypeComboBox = Objects.requireNonNull(serverAuthTypeComboBox);
        this.userNameField = Objects.requireNonNull(userNameField);
        this.passwordField = Objects.requireNonNull(passwordField);

        encryptTypeComboBox.addActionListener(e -> {
            ServerEncryptType selected = (ServerEncryptType) encryptTypeComboBox.getSelectedItem();
            if (selected == null) {
                return;
            }
            switch (selected) {
                case NONE:
                case TLS_12_CA:
                    certPortField.setValue(0);
                    ((JSpinner.DefaultEditor) certPortField.getEditor()).getTextField().setEditable(false);
                    break;
                case TLS_12:
                    certPortField.setValue(7060);
                    ((JSpinner.DefaultEditor) certPortField.getEditor()).getTextField().setEditable(true);
                    break;
            }
        });

        serverAuthTypeComboBox.addActionListener(e -> {
            ServerAuthType selected = (ServerAuthType) serverAuthTypeComboBox.getSelectedItem();
            if (selected == null) {
                return;
            }
            switch (selected) {
                case COMMON:
                    userNameField.setText("");
                    userNameField.setEditable(false);
                    break;
                case USER:
                    userNameField.setEditable(true);
                    break;
            }
        });
    }

    public String getHostName() {
        return hostNameField.getText();
    }

    public void setHostName(String hostName) {
        if (hostName == null || hostName.isBlank()) {
            this.hostNameField.setText("");
            return;
        }
        this.hostNameField.setText(hostName);
    }

    public Integer getPort() {
        Object o = portField.getValue();
        return o != null ? Integer.parseInt(o.toString()) : null;
    }

    public void setPort(Integer port) {
        if (port == null) {
            portField.setValue(443);
            return;
        }
        portField.setValue(port);
    }

    public Integer getCertPort() {
        Object o = certPortField.getValue();
        return o != null ? Integer.parseInt(o.toString()) : null;
    }

    public void setCertPort(Integer port) {
        if (port == null) {
            certPortField.setValue(7060);
            return;
        }
        certPortField.setValue(port);
    }

    public ServerEncryptType getEncryptType() {
        return (ServerEncryptType) encryptTypeComboBox.getSelectedItem();
    }

    public void setEncryptType(ServerEncryptType type) {
        encryptTypeComboBox.setSelectedItem(type);
    }

    public ServerAuthType getServerAuthType() {
        return (ServerAuthType) serverAuthTypeComboBox.getSelectedItem();
    }

    public void setServerAuthType(ServerAuthType type) {
        serverAuthTypeComboBox.setSelectedItem(type);
    }

    public String getUserName() {
        return userNameField.getText();
    }

    public void setUserName(String userName) {
        userNameField.setText(userName);
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public void setPassword(String password) {
        passwordField.setText(password);
    }

    public void refresh(ServerVO vo) {
        setHostName(vo.getHostName());
        setPort(vo.getPort());
        setCertPort(vo.getCertPort());
        setServerAuthType(vo.getAuthType());
        setEncryptType(vo.getEncryptType());
        setUserName(vo.getUserName());
        setPassword(vo.getPassword());
    }

    public void clean() {
        setHostName("");
        setPort(0);
        setCertPort(0);
        setServerAuthType(ServerAuthType.COMMON);
        setEncryptType(ServerEncryptType.NONE);
        setUserName("");
        setPassword("");
    }


    public void setEditable(boolean editable) {
        hostNameField.setEditable(editable);
        ((JSpinner.DefaultEditor) portField.getEditor()).getTextField().setEditable(editable);
        ((JSpinner.DefaultEditor) certPortField.getEditor()).getTextField().setEditable(editable);
        serverAuthTypeComboBox.setEditable(editable);
        encryptTypeComboBox.setEditable(editable);
        userNameField.setEditable(editable);
        passwordField.setEditable(editable);
    }


    public static class Builder {

        private JTextField hostNameField;

        private JSpinner portField;

        private JSpinner certPortField;

        private JComboBox<ServerEncryptType> encryptTypeComboBox;

        private JComboBox<ServerAuthType> serverAuthTypeComboBox;

        private JTextField userNameField;

        private JPasswordField passwordField;

        public Builder hostNameField(JTextField hostNameField) {
            this.hostNameField = hostNameField;
            return this;
        }

        public Builder portField(JSpinner portField) {
            this.portField = portField;
            return this;
        }

        public Builder certPortField(JSpinner certPortField) {
            this.certPortField = certPortField;
            return this;
        }

        public Builder encryptTypeComboBox(JComboBox<ServerEncryptType> encryptTypeComboBox) {
            this.encryptTypeComboBox = encryptTypeComboBox;
            return this;
        }

        public Builder serverAuthTypeComboBox(JComboBox<ServerAuthType> serverAuthTypeComboBox) {
            this.serverAuthTypeComboBox = serverAuthTypeComboBox;
            return this;
        }

        public Builder userNameField(JTextField userNameField) {
            this.userNameField = userNameField;
            return this;
        }

        public Builder passwordField(JPasswordField passwordField) {
            this.passwordField = passwordField;
            return this;
        }

        public ServerConfigureUIContainer build() {
            return new ServerConfigureUIContainer(hostNameField, portField, certPortField, encryptTypeComboBox,
                    serverAuthTypeComboBox, userNameField, passwordField);
        }
    }
}