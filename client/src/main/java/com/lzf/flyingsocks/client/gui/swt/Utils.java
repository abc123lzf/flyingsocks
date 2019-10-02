package com.lzf.flyingsocks.client.gui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

import java.io.InputStream;

/**
 * @author lizifan
 * @create 2019.10.2 0:22
 * @description SWT GUI工具类
 */
abstract class Utils {

    private static final float DPI_SCALE = Display.getDefault().getDPI().x / 144.0f;

    /**
     * 调整SWT组件大小以适应操作系统DPI
     * @param composite SWT组件
     */
    static void adaptDPI(Composite composite) {
        if(composite instanceof Shell) {
            int x = (int) (composite.getLocation().x * DPI_SCALE);
            int y = (int) (composite.getLocation().y * DPI_SCALE);
            int w = (int) (composite.getSize().x * DPI_SCALE);
            int h = (int) (composite.getSize().y * DPI_SCALE);
            composite.setBounds(x, y, w, h);
        }

        for(Control control : composite.getChildren()) {
            if(control instanceof Composite) {
                adaptDPI((Composite) control);
            }

            int x = (int) (control.getLocation().x * DPI_SCALE);
            int y = (int) (control.getLocation().y * DPI_SCALE);
            int w = (int) (control.getSize().x * DPI_SCALE);
            int h = (int) (control.getSize().y * DPI_SCALE);
            control.setBounds(x, y, w, h);
        }
    }


    static void addButtonSelectionListener(Button button, SimpleSelectionListener listener) {
        button.addSelectionListener(listener);
    }

    static void addComboSelectionListener(Combo combo, SimpleSelectionListener listener) {
        combo.addSelectionListener(listener);
    }

    static void addListSelectionListener(List list, SimpleSelectionListener listener) {
        list.addSelectionListener(listener);
    }

    static void addMenuItemSelectionListener(MenuItem item, SimpleSelectionListener listener) {
        item.addSelectionListener(listener);
    }

    /**
     * 创建窗口
     * @param display SWT Display
     * @param title 标题
     * @param icon 窗口图标
     * @param width 宽度
     * @param height 高度
     * @return 窗口对象
     */
    static Shell createShell(Display display, String title, Image icon, int width, int height) {
        Shell sh = new Shell(display);
        sh.setText(title);
        sh.setSize(width, height);
        sh.setVisible(false);
        if(icon != null) {
            sh.setImage(icon);
        }

        sh.addListener(SWT.Close, e -> {
            e.doit = false;
            sh.setVisible(false);
        });

        return sh;
    }


    static Combo createCombo(Shell shell, int x, int y, int width, int height) {
        Combo c = new Combo(shell, SWT.READ_ONLY);
        c.setBounds(x, y, width, height);
        return c;
    }


    /**
     * 载入图片
     * @param is 图片输入流
     * @return SWT图片
     */
    static Image loadImage(InputStream is) {
        return new Image(null, is);
    }

    /**
     * 创建文本栏
     * @param shell 窗口
     * @param text 文本
     * @param x 左上角横坐标
     * @param y 左上角纵坐标
     * @param width 宽度
     * @param height 高度
     * @param style 样式
     */
    static void createLabel(Shell shell, String text, int x, int y, int width, int height, int style) {
        Label l = new Label(shell, style);
        l.setBounds(x, y, width, height);
        if(text != null) {
            l.setText(text);
        }
    }

    static void createImageLabel(Shell shell, Image image, int x, int y, int width, int height) {
        Label l = new Label(shell, SWT.CENTER);
        l.setBounds(x, y, width, height);
        l.setImage(image);
    }


    static Button createButton(Shell shell, String text, int x, int y, int width, int height) {
        Button b = new Button(shell, SWT.PUSH);
        b.setText(text);
        b.setBounds(x, y, width, height);
        return b;
    }


    static Button createRadio(Shell shell, String text, int x, int y, int width, int height) {
        Button b = new Button(shell, SWT.RADIO);
        b.setText(text);
        b.setBounds(x, y, width, height);
        return b;
    }


    /**
     * 发布提示框
     * @param shell 窗口对象
     * @param title 标题
     * @param content 内容
     * @param style 样式
     */
    static void showMessageBox(Shell shell, String title, String content, int style) {
        MessageBox box = new MessageBox(shell, style);
        box.setText(title);
        box.setMessage(content);
        box.open();
    }

    /**
     * 创建菜单项
     * @param menu 菜单
     * @param text 菜单项文本
     * @param listener 点击时监听器
     */
    static void createMenuItem(Menu menu, String text, SimpleSelectionListener listener) {
        MenuItem it = new MenuItem(menu, SWT.PUSH);
        it.setText(text);
        if(listener != null) {
            it.addSelectionListener(listener);
        }
    }

    /**
     * 创建级联菜单项
     * @param menu 菜单
     * @param text 菜单项文本
     * @param listener 点击时监听器
     */
    static void createCascadeMenuItem(Menu menu, String text, SimpleSelectionListener listener) {
        MenuItem it = new MenuItem(menu, SWT.CASCADE);
        it.setText(text);
        if(listener != null) {
            it.addSelectionListener(listener);
        }
    }

    /**
     * 创建菜单分隔线
     * @param menu 菜单
     */
    static void createMenuSeparator(Menu menu) {
        new MenuItem(menu, SWT.SEPARATOR);
    }


    private Utils() {
        throw new UnsupportedOperationException();
    }
}
