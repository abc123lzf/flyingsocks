package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import com.lzf.flyingsocks.client.proxy.socks.SocksConfig;
import com.lzf.flyingsocks.util.BaseUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static com.lzf.flyingsocks.client.gui.swt.Utils.*;

/**
 * @author lizifan 695199262@qq.com
 * @since 2019.9.10
 * Socks5代理设置界面
 */
final class SocksSettingModule extends AbstractModule<SWTViewComponent> {

    private final ClientOperator operator;

    private final Shell shell;

    SocksSettingModule(SWTViewComponent component, Display display) {
        super(Objects.requireNonNull(component));
        this.operator = component.getParentComponent();

        Image icon;
        try (InputStream is = ResourceManager.openIconImageStream()){
            icon = loadImage(is);
        } catch (IOException e) {
            throw new Error(e);
        }

        this.shell = createShell(display, "Socks5本地代理设置", icon, 600, 250);
        initial();
        adaptDPI(shell);
    }

    private void initial() {
        createLabel(shell, "验证", 20, 5, 80, 30, SWT.CENTER);
        createLabel(shell, "用户名", 20, 40, 80, 30, SWT.CENTER);
        createLabel(shell, "密码", 20, 75, 80, 30, SWT.CENTER);
        createLabel(shell, "代理端口", 20, 110, 80, 30, SWT.CENTER);

        Button open = createRadio(shell, "打开", 160, 5, 80, 30);
        Button off = createRadio(shell, "关闭", 250, 5, 80, 30);

        Text user = new Text(shell, SWT.BORDER);
        user.setBounds(160, 40, 380, 30);
        Text pass = new Text(shell, SWT.BORDER | SWT.PASSWORD);
        pass.setBounds(160, 75, 380, 30);
        Text port = new Text(shell, SWT.BORDER);
        port.setBounds(160, 110, 130, 30);

        Button enter = createButton(shell, "确认", 170, 145, 150, 30);
        addButtonSelectionListener(enter, e -> {
            boolean auth = open.getSelection();
            String username = user.getText();
            String password = pass.getText();
            int p;
            if(BaseUtils.isPortString(port.getText())) {
                p = Integer.parseInt(port.getText());
            } else {
                showMessageBox(shell, "提示", "端口不合法", SWT.ICON_ERROR | SWT.OK);
                return;
            }

            operator.updateSocksProxyAuthentication(p, auth, username, password);
            SocksConfig cfg = operator.getSocksConfig();
            if(cfg.getPort() != p) {
                showMessageBox(shell, "提示", "修改完成, 代理端口的修改需要重启才可生效", SWT.ICON_INFORMATION | SWT.OK);
            } else {
                showMessageBox(shell, "提示", "修改完成", SWT.ICON_INFORMATION | SWT.OK);
            }
        });

        addButtonSelectionListener(open, e -> {
            user.setEditable(true);
            pass.setEditable(true);
        });

        addButtonSelectionListener(off, e -> {
            user.setEditable(false);
            pass.setEditable(false);
        });

        Button cancel = createButton(shell, "取消", 360, 145, 150, 30);
        addButtonSelectionListener(cancel, e -> setVisiable(false));

        SocksConfig cfg = operator.getSocksConfig();
        if(cfg.isAuth()) {
            open.setSelection(true);
        } else {
            off.setSelection(true);
            user.setEditable(false);
            pass.setEditable(false);
        }

        if(cfg.getUsername() != null) {
            user.setText(cfg.getUsername());
        }

        if(cfg.getPassword() != null) {
            pass.setText(cfg.getPassword());
        }

        port.setText(String.valueOf(cfg.getPort()));
    }

    void setVisiable(boolean visiable) {
        shell.setVisible(visiable);
    }
}
