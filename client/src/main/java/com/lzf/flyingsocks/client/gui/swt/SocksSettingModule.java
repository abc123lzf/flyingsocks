package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.*;

import java.io.IOException;
import java.util.Objects;

/**
 * Socks5代理设置界面
 */
final class SocksSettingModule extends AbstractModule<SWTViewComponent> {

    private final Display display;

    private final ClientOperator operator;

    private final Shell shell;

    SocksSettingModule(SWTViewComponent component, Display display) {
        super(Objects.requireNonNull(component));
        this.display = display;
        this.operator = component.getParentComponent();
        this.shell = initial();
    }

    private Shell initial() {
        Shell shell = new Shell(display);
        shell.setText("Socks5本地代理设置");
        shell.setSize(600, 330);
        try {
            shell.setImage(new Image(display, new ImageData(ResourceManager.openSystemTrayImageStream())));
        } catch (IOException e) {
            throw new Error(e);
        }

        shell.addListener(SWT.Close, e -> {
            setVisiable(false);
            e.doit = false;
        });

        Label openl = new Label(shell, SWT.CENTER ^ SWT.RIGHT);
        openl.setText("验证");
        openl.setBounds(20, 5, 80, 50);

        Button open = new Button(shell, SWT.RADIO);
        open.setText("打开");
        open.setBounds(160, 5, 80, 50);
        Button off = new Button(shell, SWT.RADIO);
        off.setText("关闭");
        off.setBounds(250, 5, 80, 50);

        Label userl = new Label(shell, SWT.CENTER ^ SWT.RIGHT);
        userl.setText("用户名");
        userl.setBounds(20, 60, 80, 50);

        Text user = new Text(shell, SWT.BORDER);
        user.setBounds(160, 60, 380, 50);

        Label passl = new Label(shell, SWT.CENTER ^ SWT.RIGHT);
        passl.setText("密码");
        passl.setBounds(20, 130, 80, 50);

        Text pass = new Text(shell, SWT.BORDER ^ SWT.PASSWORD);
        pass.setBounds(160, 130, 380, 50);

        Button enter = new Button(shell, SWT.NONE);
        enter.setText("确认");
        enter.setBounds(170, 200, 150, 50);
        enter.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean auth = open.getSelection();
                String username = user.getText();
                String password = user.getText();
                operator.updateSocksProxyAuthentication(auth, username, password);
            }
        });

        open.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                user.setEditable(true);
                pass.setEditable(true);
            }
        });

        off.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                user.setEditable(false);
                pass.setEditable(false);
            }
        });

        Button cancel = new Button(shell, SWT.NONE);
        cancel.setText("取消");
        cancel.setBounds(360, 200, 150, 50);
        cancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setVisiable(false);
            }
        });

        shell.setVisible(false);

        return shell;
    }

    void setVisiable(boolean visiable) {
        shell.setVisible(visiable);
    }
}
