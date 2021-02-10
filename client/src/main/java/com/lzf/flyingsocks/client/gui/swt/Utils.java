/*
 * Copyright (c) 2019 abc123lzf <abc123lzf@126.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lzf.flyingsocks.client.gui.swt;

import io.netty.util.internal.PlatformDependent;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.jfree.swt.SWTUtils;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * SWT GUI工具类
 *
 * @author lizifan
 * @since 2019.10.2 0:22
 */
abstract class Utils {

    /**
     * DPI校准值，决定UI大小，会自动根据系统DPI调整
     * 如果因为DPI不准确导致UI过大或者过小时，启动时请配置VM参数flyingsocks.basedpi
     */
    private static final float DPI_SCALE;

    /**
     * GUI国际化
     */
    private static final ResourceBundle RESOURCE_BUNDLE;


    static {
        String baseDpiStr = System.getProperty("flyingsocks.basedpi", "144");
        float base = Float.parseFloat(baseDpiStr);
        float result;

        // MacOS通过Display得到的DPI不准确
        if (isMacOS()) {
            try {
                GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                Class<? extends GraphicsDevice> deviceClass = device.getClass();
                Method getScaleFactor = deviceClass.getDeclaredMethod("getScaleFactor");
                int scaleFactor = (Integer) getScaleFactor.invoke(device);
                result = scaleFactor * 0.75f / 2.0f;
            } catch (Exception e) {
                System.err.println("Can not get scale factor, cause:" + e.getMessage());
                result = 1;
            }
        } else {
            result = Display.getDefault().getDPI().x / base;
        }

        DPI_SCALE = result;
        Locale locale = Locale.getDefault();
        RESOURCE_BUNDLE = ResourceBundle.getBundle("META-INF/i18n/swtui", locale);
    }

    /**
     * @return 是否是MacOS系统
     */
    static boolean isMacOS() {
        return PlatformDependent.isOsx();
    }


    static int adaptDPI(int pixel) {
        return (int) (DPI_SCALE * pixel);
    }


    /**
     * 调整SWT组件大小以适应操作系统DPI
     *
     * @param composite SWT组件
     */
    static void adaptDPI(Composite composite) {
        if (composite instanceof Shell) {
            adaptControlDPI(composite);
        }

        for (Control control : composite.getChildren()) {
            if (control instanceof Composite) {
                adaptDPI((Composite) control);
            }

            adaptControlDPI(control);
        }
    }

    private static void adaptControlDPI(Control control) {
        int x = (int) (control.getLocation().x * DPI_SCALE);
        int y = (int) (control.getLocation().y * DPI_SCALE);
        int w = (int) (control.getSize().x * DPI_SCALE);
        int h = (int) (control.getSize().y * DPI_SCALE);
        control.setBounds(x, y, w, h);
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
     *
     * @param display SWT Display
     * @param title   标题
     * @param icon    窗口图标
     * @param width   宽度
     * @param height  高度
     * @return 窗口对象
     */
    static Shell createShell(Display display, String title, Image icon, int width, int height) {
        Shell sh = new Shell(display, (SWT.SHELL_TRIM & (~SWT.RESIZE)) | SWT.MIN | SWT.ON_TOP);
        sh.setText(i18n(title));
        sh.setSize(width, height);
        sh.setVisible(false);
        if (icon != null) {
            sh.setImage(icon);
        }

        sh.addListener(SWT.Close, e -> {
            e.doit = false;
            sh.setVisible(false);
        });

        return sh;
    }


    static Combo createCombo(Composite shell, int x, int y, int width, int height) {
        Combo c = new Combo(shell, SWT.READ_ONLY);
        c.setBounds(x, y, width, height);
        return c;
    }


    /**
     * 载入图片
     *
     * @param is 图片输入流
     * @return SWT图片
     */
    static Image loadImage(InputStream is) {
        return new Image(null, is);
    }

    /**
     * 构造颜色
     */
    static Color createColor(int r, int g, int b) {
        return new Color(null, r, g, b);
    }

    /**
     * 创建文本栏
     *
     * @param comp   窗口
     * @param text   文本
     * @param x      左上角横坐标
     * @param y      左上角纵坐标
     * @param width  宽度
     * @param height 高度
     * @param style  样式
     */
    static Label createLabel(Composite comp, String text, int x, int y, int width, int height, int style) {
        text = i18n(text);
        Label l = new Label(comp, style);
        l.setBounds(x, y, width, height);
        if (text != null) {
            l.setText(text);
        }
        return l;
    }

    static void createLabel(Composite comp, Image image, int x, int y, int width, int height) {
        Label l = new Label(comp, SWT.CENTER);
        l.setBounds(x, y, width, height);
        l.setImage(image);
    }


    static Canvas createCanvas(Composite comp, BufferedImage image, int x, int y, int width, int height) {
        Canvas canvas = new Canvas(comp, SWT.CENTER);
        canvas.setBounds(x, y, width, height);
        canvas.setBackgroundImage(new Image(null, SWTUtils.convertAWTImageToSWT(image)));
        return canvas;
    }


    static void refreshCanvas(Canvas canvas, BufferedImage image) {
        Image now = canvas.getBackgroundImage();
        canvas.setBackgroundImage(new Image(null, SWTUtils.convertAWTImageToSWT(image)));
        canvas.redraw();
        if (now != null) {
            now.dispose();
        }
    }



    static Button createButton(Composite comp, String text, int x, int y, int width, int height) {
        Button b = new Button(comp, SWT.PUSH);
        b.setText(i18n(text));
        b.setBounds(x, y, width, height);
        return b;
    }


    static Button createRadio(Composite comp, String text, int x, int y, int width, int height) {
        Button b = new Button(comp, SWT.RADIO);
        b.setText(i18n(text));
        b.setBounds(x, y, width, height);
        return b;
    }


    /**
     * 发布提示框
     *
     * @param shell   窗口对象
     * @param title   标题
     * @param content 内容
     * @param style   样式
     */
    static void showMessageBox(Shell shell, String title, String content, int style) {
        title = i18n(title);
        content = i18n(content);

        MessageBox box = new MessageBox(shell, style);
        box.setText(title);
        box.setMessage(content);
        box.open();
    }

    /**
     * 创建菜单项
     *
     * @param menu     菜单
     * @param text     菜单项文本
     * @param listener 点击时监听器
     */
    static void createMenuItem(Menu menu, String text, SimpleSelectionListener listener) {
        MenuItem it = new MenuItem(menu, SWT.PUSH);
        it.setText(i18n(text));
        if (listener != null) {
            it.addSelectionListener(listener);
        }
    }

    /**
     * 创建级联菜单项
     *
     * @param menu     菜单
     * @param text     菜单项文本
     * @param listener 点击时监听器
     */
    static void createCascadeMenuItem(Menu menu, String text, SimpleSelectionListener listener) {
        MenuItem it = new MenuItem(menu, SWT.CASCADE);
        it.setText(i18n(text));
        if (listener != null) {
            it.addSelectionListener(listener);
        }
    }


    static String i18n(String key) {
        if (!StringUtils.isBlank(key) && RESOURCE_BUNDLE.containsKey(key)) {
            return RESOURCE_BUNDLE.getString(key);
        }
        return key;
    }


    /**
     * 创建菜单分隔线
     *
     * @param menu 菜单
     */
    static void createMenuSeparator(Menu menu) {
        new MenuItem(menu, SWT.SEPARATOR);
    }

    private Utils() {
        throw new UnsupportedOperationException();
    }
}
