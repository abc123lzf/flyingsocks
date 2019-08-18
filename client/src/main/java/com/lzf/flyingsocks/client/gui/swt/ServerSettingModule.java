package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.client.gui.GUIResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.IOException;
import java.util.Objects;

/**
 * @create 2019.8.17 23:05
 * @description 服务器设置界面
 */
final class ServerSettingModule extends AbstractModule<SWTViewComponent> {

    private final Display display;

    private final Shell shell;

    ServerSettingModule(SWTViewComponent component, Display display) {
        super(Objects.requireNonNull(component));
        this.display = display;
        this.shell = initial();
    }


    private final class ServerList {

    }


    private Shell initial() {
        final Shell shell = new Shell(display);
        shell.setText("服务器设置");
        shell.setSize(800, 400);
        shell.setVisible(false);
        shell.setLayout(null);
        try {
            shell.setImage(new Image(display, new ImageData(GUIResourceManager.openIconImageStream())));
        } catch (IOException e) {
            throw new Error(e);
        }


        Button enter = new Button(shell, SWT.NONE);
        enter.setText("保存");
        enter.setBounds(400, 300, 100, 100);


        shell.open();
        return shell;
    }


    Shell getShell() {
        return shell;
    }
}
