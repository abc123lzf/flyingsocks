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
package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.Component;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.Environment;
import com.lzf.flyingsocks.TopLevelComponent;
import com.lzf.flyingsocks.VoidComponent;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;

import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public abstract class Client extends TopLevelComponent
        implements Component<VoidComponent>, Environment, ClientOperator {

    /**
     * 默认组件名
     */
    static final String DEFAULT_COMPONENT_NAME = "flyingsocks-client";


    private static final ResourceBundle EXIT_MSG_BUNDLE =
            ResourceBundle.getBundle("META-INF/i18n/exitmsg", Locale.getDefault());


    /**
     * GUI事件处理循环
     */
    private Runnable guiTask;


    Client() {
        super(DEFAULT_COMPONENT_NAME);
    }

    /**
     * @return 配置管理器
     */
    public final ConfigManager<?> getConfigManager() {
        return super.getConfigManager();
    }


    @Override
    public void cleanLogFiles() {
        Enumeration<?> appenders = Logger.getRootLogger().getAllAppenders();
        while (appenders.hasMoreElements()) {
            Appender appender = (Appender) appenders.nextElement();
            if (appender instanceof FileAppender) {
                Path path = Paths.get(((FileAppender) appender).getFile());
                try {
                    if (Files.exists(path) && Files.isRegularFile(path)) {
                        Files.delete(path);
                    }
                } catch (IOException e) {
                    log.warn("Could not delete log file [{}]", path, e);
                }
            }
        }
    }

    @Override
    public void openLogDirectory() {
        Appender appender = Logger.getRootLogger().getAppender(log.getName());
        if (appender instanceof FileAppender) {
            File folder = new File(((FileAppender) appender).getFile());
            try {
                Desktop.getDesktop().open(folder);
            } catch (IOException e) {
                log.warn("An error occurred while open log file directory", e);
            }
        }
    }

    @Override
    public void openConfigDirectory() {
        GlobalConfig gc = getConfigManager().getConfig(GlobalConfig.NAME, GlobalConfig.class);
        if (gc != null) {
            try {
                Desktop.getDesktop().open(gc.configPath().toFile());
            } catch (IOException e) {
                log.warn("Open config file directory occur a exception", e);
            }
        }
    }


    @Override
    public void openBrowser(String url) {
        try {
            if (isWindows()) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else {
                Desktop dt = Desktop.getDesktop();
                if (dt.isSupported(Desktop.Action.BROWSE)) {
                    dt.browse(new URI(url));
                }
            }
        } catch (IOException | URISyntaxException e) {
            log.warn("Open browser occur a exception", e);
        }
    }

    /**
     * 设置GUI界面任务
     */
    public void setGUITask(Runnable task) {
        this.guiTask = Objects.requireNonNull(task);
    }

    /**
     * 运行GUI任务
     */
    boolean runGUITask() {
        Runnable task = this.guiTask;
        if (task != null) {
            task.run();
            return true;
        }

        return false;
    }


    public static void exitWithNotify(int status, String message) {
        if (Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(null, message != null ? message : "", "ERROR", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(status);
    }


    public static void exitWithNotify(int status, String string, Object... args) {
        if (Desktop.isDesktopSupported()) {
            String msg;
            if (EXIT_MSG_BUNDLE.containsKey(string)) {
                msg = EXIT_MSG_BUNDLE.getString(string);
            } else {
                msg = string;
            }

            if (args == null || args.length == 0) {
                JOptionPane.showMessageDialog(null, msg,
                        EXIT_MSG_BUNDLE.getString("exitmsg.title"), JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, MessageFormat.format(msg, args),
                        EXIT_MSG_BUNDLE.getString("exitmsg.title"), JOptionPane.ERROR_MESSAGE);
            }
        }

        System.exit(status);
    }

}
