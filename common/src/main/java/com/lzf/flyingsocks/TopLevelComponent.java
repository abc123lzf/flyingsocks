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
package com.lzf.flyingsocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * 表示顶层组件，顶层组件不可拥有父组件
 * 顶层组件除了具有AbstractComponent所有特性外，还可以保存环境参数，加载环境资源，并具有配置管理器
 *
 * @see com.lzf.flyingsocks.AbstractComponent
 * @see com.lzf.flyingsocks.Environment
 */
public abstract class TopLevelComponent extends AbstractComponent<VoidComponent> implements Environment {

    static {
        try {
            //加载这个类以便支持‘classpath:’类型的URL
            Class.forName("com.lzf.flyingsocks.url.ClasspathURLHandlerFactory");
        } catch (ClassNotFoundException e) {
            throw new ComponentException(e);
        }
    }

    protected final String version;

    /**
     * 配置管理器实例
     */
    private final ConfigManager<TopLevelComponent> configManager = new DefaultConfigManager<>(this);

    protected TopLevelComponent() {
        super();
        this.version = readVersion();
        addModule(configManager);
    }

    protected TopLevelComponent(String name) {
        super(name, null);
        this.version = readVersion();
        addModule(configManager);
    }

    @Override
    public final VoidComponent getParentComponent() {
        return null;
    }

    public final String getVersion() {
        return version;
    }

    @Override
    public String getEnvironmentVariable(String key) {
        return System.getenv(key);
    }

    @Override
    public String getSystemProperties(String key) {
        return System.getProperty(key);
    }

    @Override
    public void setSystemProperties(String key, String value) {
        System.setProperty(key, value);
    }

    @Override
    public InputStream loadResource(String path) throws IOException {
        try {
            URL url = new URL(path);
            return url.openStream();
        } catch (MalformedURLException e) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return new FileInputStream(file);
            } else {
                if (log.isWarnEnabled())
                    log.warn("URL or file path error", e);
                return null;
            }
        } catch (IOException e) {
            if (log.isErrorEnabled())
                log.error("load resource {} occur a IOException", path);
            throw e;
        }
    }

    protected ConfigManager<?> getConfigManager() {
        return configManager;
    }

    private static String readVersion() {
        try (InputStream versionInputStream = new URL("classpath://META-INF/version").openStream();
             Scanner sc = new Scanner(versionInputStream)) {
            String version = sc.nextLine();
            String tag = sc.nextLine();
            return version + "-" + tag;
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}
