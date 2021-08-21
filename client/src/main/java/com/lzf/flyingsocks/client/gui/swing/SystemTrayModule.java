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
package com.lzf.flyingsocks.client.gui.swing;

import com.bulenkov.darcula.DarculaLaf;
import com.lzf.flyingsocks.client.gui.ResourceManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;


/**
 * @author lzf abc123lzf@126.com
 * @since 2021/8/21 9:50 下午
 */
public class SystemTrayModule {

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new DarculaLaf());
        SystemTray tray = SystemTray.getSystemTray();
        TrayIcon trayIcon = new TrayIcon(ImageIO.read(ResourceManager.openIconImageStream()));
        tray.add(trayIcon);

        PopupMenu menu = new PopupMenu();
        menu.add(new MenuItem("哈哈"));
        menu.add(new MenuItem("卧槽"));
        trayIcon.setPopupMenu(menu);
    }
}
