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
package com.lzf.flyingsocks.client.proxy.transparent;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.GlobalConfig;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/24 19:06
 */
public class TransparentProxyConfig extends AbstractConfig {

    public static final String NAME = "config.transparent";

    private static final int DEFAULT_PORT = 6870;

    private final Path configPath;

    private boolean enable;

    private int port;

    public TransparentProxyConfig(ConfigManager<?> configManager) {
        super(Objects.requireNonNull(configManager), NAME);
        GlobalConfig gc = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);
        this.configPath = gc.configPath().resolve("transparent-option");
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        createFileIfNotExists();
        loadFile();
    }


    public int getBindPort() {
        return port;
    }


    public boolean isEnable() {
        return enable;
    }


    private void loadFile() {
        try (FileReader reader = new FileReader(configPath.toFile())) {
            Properties properties = new Properties();
            properties.load(reader);
            this.enable = Boolean.parseBoolean(properties.getProperty("enable", "false"));
            this.port = Integer.parseInt(properties.getProperty("port", Integer.toString(DEFAULT_PORT)));
        } catch (IOException e) {
            throw new ConfigInitializationException(e);
        }
    }


    private void createFileIfNotExists() {
        if (Files.isRegularFile(configPath)) {
            return;
        }

        if (Files.isDirectory(configPath)) {
            throw new ConfigInitializationException("Config [" + configPath + "] is Directory!");
        }

        Properties properties = new Properties();
        properties.put("enable", Boolean.toString(false));
        properties.put("port", Integer.toString(DEFAULT_PORT));

        try (FileWriter writer = new FileWriter(configPath.toFile())) {
            properties.store(writer, "flyingsocks transparent configuration (ONLY LINUX)");
        } catch (IOException e) {
            throw new ConfigInitializationException(e);
        }
    }


    @Override
    public void save() throws Exception {
        Properties properties = new Properties();
        properties.put("enable", Boolean.toString(this.enable));
        properties.put("port", Integer.toString(this.port));
        try (FileWriter writer = new FileWriter(configPath.toFile())) {
            properties.store(writer, "flyingsocks transparent configuration (ONLY LINUX)");
        } catch (IOException e) {
            throw new ConfigInitializationException(e);
        }
    }

}
