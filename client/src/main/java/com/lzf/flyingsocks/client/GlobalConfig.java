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

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 初始配置文件，用于获取用户配置文件路径、GUI设置以及应用程序超时时间
 */
public class GlobalConfig extends AbstractConfig {

    public static final String NAME = "config.global";

    private static final int DEFAULT_CONNECT_TIMEOUT = 8000;

    private static final String FILE_NAME = "global-options";

    /**
     * 配置文件所在目录
     */
    private Path path;

    /**
     * 是否开启GUI，对于Linux命令行则无需打开GUI
     */
    private boolean enableGUI;

    /**
     * 是否开启Socks5代理服务端口
     */
    private boolean enableSocksProxy;

    /**
     * 是否开启HTTP代理服务端口
     */
    private boolean enableHttpProxy;

    /**
     * 是否打开透明代理 (目前仅Linux支持)
     */
    private boolean enableTransparentProxy;


    /**
     * 应用程序连接超时时间
     */
    private int connectTimeout;


    GlobalConfig(ConfigManager<?> configManager) {
        super(configManager, NAME);
    }

    /**
     * 加载基本配置文件，并初始化用户存档文件
     *
     * @throws ConfigInitializationException 如果无法获取基本配置文件或者无法创建用户存档文件
     */
    @Override
    protected void initInternal() throws ConfigInitializationException {
        String location;

        if (configManager.isWindows()) {
            location = configManager.getSystemProperties("config.location.windows");
        } else if (configManager.isMacOS()) {
            location = configManager.getSystemProperties("config.location.mac");
        } else {
            location = configManager.getSystemProperties("config.location.linux");
        }

        location = StringSubstitutor.replaceSystemProperties(location);

        File folder = new File(location);
        if (!folder.exists() && !folder.mkdirs()) {
            throw new ConfigInitializationException("Can not create folder at " + folder.getAbsolutePath());
        }

        Path path = Paths.get(location);
        this.path = path;

        Path filePath = path.resolve(FILE_NAME);
        createFileIfNotExists(filePath);
        loadFile(filePath);
    }

    /**
     * @return 配置文件目录
     */
    public Path configPath() {
        return path;
    }

    /**
     * @return 是否开启GUI
     */
    public boolean isEnableGUI() {
        return enableGUI;
    }

    /**
     * @return 是否开启HTTP代理服务端口
     */
    public boolean isEnableHttpProxy() {
        return enableHttpProxy;
    }

    public void setEnableHttpProxy(boolean enableHttpProxy) {
        boolean enable = this.enableHttpProxy;
        if (enable == enableHttpProxy) {
            return;
        }
        this.enableHttpProxy = enableHttpProxy;
        configManager.updateConfig(this);
    }

    /**
     * @return 是否开启Socks5代理服务端口
     */
    public boolean isEnableSocksProxy() {
        return enableSocksProxy;
    }

    /**
     * @return 是否开启透明代理
     */
    public boolean isEnableTransparentProxy() {
        return enableTransparentProxy;
    }


    public void setEnableTransparentProxy(boolean enableTransparentProxy) {
        boolean enable = this.enableHttpProxy;
        if (enable == enableTransparentProxy) {
            return;
        }
        this.enableTransparentProxy = enableTransparentProxy;
        configManager.updateConfig(this);
    }

    /**
     * @return 应用程序连接超时时间
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }


    private void createFileIfNotExists(Path path) {
        if (Files.exists(path)) {
            return;
        }

        if (Files.isDirectory(path)) {
            throw new ConfigInitializationException("Path [" + path + "] is Directory!");
        }

        Properties properties = new Properties();
        properties.put("enable-gui", Boolean.toString(configManager.isMacOS() || configManager.isWindows()));
        properties.put("enable-socks5", Boolean.toString(true));
        properties.put("enable-http", Boolean.toString(false));
        properties.put("enable-transparent", Boolean.toString(false));
        properties.put("connect-timeout", String.valueOf(DEFAULT_CONNECT_TIMEOUT));
        try (FileWriter writer = new FileWriter(path.toFile())) {
            properties.store(writer, "flyingsocks base configuration");
        } catch (IOException e) {
            throw new ConfigInitializationException("Can not initialize global config file", e);
        }
    }


    private void loadFile(Path path) {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader(path.toFile())) {
            properties.load(reader);
        } catch (IOException e) {
            throw new ConfigInitializationException("Can not load global config file", e);
        }

        this.enableGUI = Boolean.parseBoolean(properties.getProperty("enable-gui",
                Boolean.toString(configManager.isMacOS() || configManager.isWindows())));
        this.enableSocksProxy = Boolean.parseBoolean(properties.getProperty("enable-socks5"));
        this.enableHttpProxy = Boolean.parseBoolean(properties.getProperty("enable-http"));
        this.enableTransparentProxy = Boolean.parseBoolean(properties.getProperty("enable-transparent"));
        this.connectTimeout = Integer.parseInt(properties.getProperty("connect-timeout", Integer.toString(DEFAULT_CONNECT_TIMEOUT)));
    }

    @Override
    public boolean canSave() {
        return true;
    }

    @Override
    public void save() throws Exception {
        Properties properties = new Properties();
        properties.put("enable-gui", Boolean.toString(this.enableGUI));
        properties.put("enable-socks5", Boolean.toString(this.enableSocksProxy));
        properties.put("enable-http", Boolean.toString(this.enableHttpProxy));
        properties.put("enable-transparent", Boolean.toString(this.enableTransparentProxy));
        properties.put("connect-timeout", Integer.toString(this.connectTimeout));

        Path path = this.path;
        try (FileWriter writer = new FileWriter(path.resolve(FILE_NAME).toFile())) {
            properties.store(writer, "flyingsocks base configuration");
        } catch (IOException e) {
            throw new ConfigInitializationException("Can not initialize global config file", e);
        }
    }
}
