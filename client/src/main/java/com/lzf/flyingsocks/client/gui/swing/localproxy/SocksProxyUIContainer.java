package com.lzf.flyingsocks.client.gui.swing.localproxy;

import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author lizifan
 * @since 2022.1.2 18:42
 */
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

        public Builder openButton(JRadioButton openButton) {
            this.openButton = openButton;
            return this;
        }

        public Builder closeButton(JRadioButton closeButton) {
            this.closeButton = closeButton;
            return this;
        }

        public Builder portField(JSpinner portField) {
            this.portField = portField;
            return this;
        }

        public Builder authOpenButton(JRadioButton authOpenButton) {
            this.authOpenButton = authOpenButton;
            return this;
        }

        public Builder authCloseButton(JRadioButton authCloseButton) {
            this.authCloseButton = authCloseButton;
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

        public Builder saveButton(JButton saveButton) {
            this.saveButton = saveButton;
            return this;
        }

        public Builder cancelButton(JButton cancelButton) {
            this.cancelButton = cancelButton;
            return this;
        }

        public SocksProxyUIContainer build() {
            return new SocksProxyUIContainer(openButton, closeButton, portField, authOpenButton, authCloseButton,
                    userNameField, passwordField, saveButton, cancelButton);
        }
    }
}