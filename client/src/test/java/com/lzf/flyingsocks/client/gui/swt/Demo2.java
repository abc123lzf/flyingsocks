package com.lzf.flyingsocks.client.gui.swt;

import io.netty.util.internal.PlatformDependent;

import javax.swing.*;

public class Demo2 {

    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame("flyingsocks");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        frame.setSize(800, 400);
        JTextField field = new JTextField();
        field.setBounds(10, 10, 200, 30);
        frame.add(field);
        frame.setVisible(true);

        System.out.println(PlatformDependent.javaVersion());
    }

}
