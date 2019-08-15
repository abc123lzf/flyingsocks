package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.client.Client;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import java.util.Objects;

/**
 * @author lizifan lzf@webull.com
 * @create 2019.8.13 9:40
 * @description SWT GUI组件
 */
public class SWTViewComponent extends AbstractComponent<Client> {

    private final Display display = new Display();

    private final Tray tray = display.getSystemTray();

    public SWTViewComponent(Client parent) {
        super("SWTViewComponent", Objects.requireNonNull(parent));
    }

    @Override
    protected void initInternal() {
        TrayItem item = new TrayItem(tray, SWT.NONE);
        final Shell shell = new Shell(display,SWT.SHELL_TRIM ^ SWT.MAX);
        item.setVisible(true);
        item.setToolTipText(Client.DEFAULT_COMPONENT_NAME);

        Menu menu = new Menu(shell, SWT.POP_UP);
        MenuItem exit = new MenuItem(menu, SWT.PUSH);
        exit.setText("退出(&x)");
    }

    @Override
    protected void stopInternal() {
        display.dispose();
    }

    @Override
    protected void restartInternal() {
        throw new UnsupportedOperationException("Can not restart SWT View Component");
    }
}
