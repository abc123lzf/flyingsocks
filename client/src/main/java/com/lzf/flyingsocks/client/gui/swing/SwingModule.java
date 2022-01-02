package com.lzf.flyingsocks.client.gui.swing;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.Internationalization;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Objects;

/**
 * @author lizifan 695199262@qq.com
 * @since 2021.8.21 23:26
 */
public abstract class SwingModule extends AbstractModule<SwingViewComponent> {

    protected final ClientOperator clientOperator;

    public SwingModule(SwingViewComponent component) {
        super(Objects.requireNonNull(component));
        this.clientOperator = component.getParentComponent();
        try {
            initial();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * 初始化界面
     */
    protected abstract void initial() throws Exception;

    /**
     * 设置模块可见性
     *
     * @param visiable 是否可见
     */
    public abstract void setVisiable(boolean visiable);


    protected static JMenu createCascadeMenu(JPopupMenu parent, String textKey) {
        var menu = new JMenu(Internationalization.get(textKey));
        parent.add(menu);
        return menu;
    }


    protected static JMenuItem createMenuItem(JPopupMenu menu, String textKey, Runnable onClick) {
        var item = new JMenuItem(Internationalization.get(textKey));
        if (onClick != null) {
            item.addActionListener(e -> onClick.run());
        }
        menu.add(item);
        return item;
    }

    protected static JMenuItem createMenuItem(JMenu menu, String textKey, Runnable onClick) {
        var item = new JMenuItem(Internationalization.get(textKey));
        if (onClick != null) {
            item.addActionListener(e -> onClick.run());
        }
        menu.add(item);
        return item;
    }

    protected static JCheckBoxMenuItem createCheckboxMenuItem(JMenu menu, String textKey, Runnable onClick) {
        var item = new JCheckBoxMenuItem(Internationalization.get(textKey));
        if (onClick != null) {
            item.addActionListener(e -> onClick.run());
        }
        menu.add(item);
        return item;
    }

    protected static void bindRadioButtonGroup(JRadioButton... buttons) {
        ButtonGroup group = new ButtonGroup();
        for (JRadioButton button : buttons) {
            group.add(button);
        }
    }


    protected static void containerAddComponent(Container container, Component... components) {
        for (Component component : components) {
            container.add(component);
        }
    }

    protected static JFrame createFrame(int width, int hight, String titleKey) {
        JFrame frame = new JFrame(Internationalization.get(titleKey));
        frame.setSize(width, hight);
        frame.getRootPane().putClientProperty("jetbrains.awt.windowDarkAppearance", true);
        return frame;
    }

    protected static boolean checkLocalPortAvailable(int port) {
        if (port < 0 || port > 65535) {
            return false;
        }
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }
    }
}
