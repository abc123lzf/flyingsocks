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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * 初始配置文件，用于获取用户配置文件路径、GUI设置以及应用程序超时时间
 */
public class GlobalConfig extends AbstractConfig {
    private static final Logger log = LoggerFactory.getLogger(GlobalConfig.class);

    public static final String NAME = "config.global";

    private static final int DEFAULT_CONNECT_TIMEOUT = 8000;

    private static final String CONNECT_TIMEOUT_FILE = "connect-timeout";

    private static final String GUI_OPTION_FILE = "gui-options";


    /**
     * 配置文件所在目录
     */
    private Path path;

    /**
     * 是否开启GUI，对于Linux命令行则无需打开GUI
     */
    private boolean openGUI;

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

        Path connTimeoutFilePath = path.resolve(CONNECT_TIMEOUT_FILE);
        if (Files.exists(connTimeoutFilePath)) {
            if (Files.isDirectory(connTimeoutFilePath)) {
                throw new ConfigInitializationException("File at " + connTimeoutFilePath + " is a Directory!");
            }

            try (FileInputStream is = new FileInputStream(connTimeoutFilePath.toFile());
                 Scanner sc = new Scanner(is)) {
                this.connectTimeout = Integer.parseInt(sc.next());
            } catch (IOException e) {
                throw new ConfigInitializationException("Can not open file at " + connTimeoutFilePath, e);
            } catch (NumberFormatException e) {
                this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
                log.warn("Illegal file format at {}, use the default value as connect timeout", connTimeoutFilePath);
            }
        } else {
            try {
                makeTemplateConnectTimeFile(connTimeoutFilePath);
                this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            } catch (IOException e) {
                log.warn("Can not create file at {}, use the default value as connect timeout", connTimeoutFilePath, e);
            }
        }

        Path guiFilePath = path.resolve(GUI_OPTION_FILE);
        if (Files.exists(guiFilePath)) {
            if (Files.isDirectory(guiFilePath)) {
                throw new ConfigInitializationException("File at " + guiFilePath + " is a Directory!");
            }

            try (FileInputStream is = new FileInputStream(guiFilePath.toFile());
                 Scanner sc = new Scanner(is)) {
                this.openGUI = sc.nextBoolean();
            } catch (IOException e) {
                throw new ConfigInitializationException("Can not open file at " + guiFilePath, e);
            } catch (NoSuchElementException e) {
                this.openGUI = true;
                log.warn("Illegal file format at {}, use the default value as gui option", guiFilePath);
            }
        } else {
            try {
                makeTemplateGUIOptionFile(guiFilePath);
                this.openGUI = true;
            } catch (IOException e) {
                throw new ConfigInitializationException("Can not create file at " + guiFilePath, e);
            }
        }
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
    public boolean isOpenGUI() {
        return openGUI;
    }

    /**
     * @return 应用程序连接超时时间
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 创建一个默认的记录connectTimeout的文件
     *
     * @param path 文件路径
     */
    private void makeTemplateConnectTimeFile(Path path) throws IOException {
        String content = String.valueOf(DEFAULT_CONNECT_TIMEOUT);
        ByteBuffer buf = ByteBuffer.allocate(content.length());
        buf.put(content.getBytes(StandardCharsets.US_ASCII));
        writeFile(path, buf);
    }

    /**
     * 创建默认GUI设置文件
     *
     * @param path 文件路径
     * @throws IOException 当写入失败
     */
    private void makeTemplateGUIOptionFile(Path path) throws IOException {
        String content = "true";
        ByteBuffer buf = ByteBuffer.allocate(content.length());
        buf.put(content.getBytes(StandardCharsets.US_ASCII));
        writeFile(path, buf);
    }

    private void writeFile(Path path, ByteBuffer buf) throws IOException {
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            buf.rewind();
            ch.write(buf);
        }
    }
}
