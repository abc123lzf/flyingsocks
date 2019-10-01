package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static com.lzf.flyingsocks.client.gui.swt.Utils.*;

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

        Image icon;
        try (InputStream is = ResourceManager.openIconImageStream()){
            icon = loadImage(is);
        } catch (IOException e) {
            throw new Error(e);
        }

        this.shell = createShell(display, "Socks5本地代理设置", icon, 600, 300);
        initial();
    }

    private void initial() {
        createLabel(shell, "验证", 20, 5, 80, 50, SWT.CENTER);
        createLabel(shell, "用户名", 20, 60, 80, 50, SWT.CENTER);
        createLabel(shell, "密码", 20, 130, 80, 50, SWT.CENTER);
        Button open = createRadio(shell, "打开", 160, 5, 80, 50);
        Button off = createRadio(shell, "关闭", 250, 5, 80, 50);

        Text user = new Text(shell, SWT.BORDER);
        user.setBounds(160, 60, 380, 50);
        Text pass = new Text(shell, SWT.BORDER | SWT.PASSWORD);
        pass.setBounds(160, 130, 380, 50);

        Button enter = createButton(shell, "确认", 170, 200, 150, 50);
        addButtonSelectionListener(enter, e -> {
            boolean auth = open.getSelection();
            String username = user.getText();
            String password = user.getText();
            operator.updateSocksProxyAuthentication(auth, username, password);
        });

        addButtonSelectionListener(open, e -> {
            user.setEditable(true);
            pass.setEditable(true);
        });

        addButtonSelectionListener(off, e -> {
            user.setEditable(false);
            pass.setEditable(false);
        });

        Button cancel = createButton(shell, "取消", 360, 200, 150, 50);
        addButtonSelectionListener(cancel, e -> setVisiable(false));
    }

    void setVisiable(boolean visiable) {
        shell.setVisible(visiable);
    }
}
