package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * 初始配置文件，用于获取用户配置文件路径、GUI设置以及应用程序超时时间
 */
public class GlobalConfig extends AbstractConfig {
    private static final Logger log = LoggerFactory.getLogger(GlobalConfig.class);

    public static final String NAME = "config.global";

    private static final String PATH = "classpath://config.properties";

    private static final int DEFAULT_CONNECT_TIMEOUT = 8000;

    private static final String CONNECT_TIMEOUT_FILE = "connect-timeout";

    private static final String GUI_OPTION_FILE = "gui-options";

    /**
     * 用户配置文件路径
     */
    private String location;

    /**
     * 配置文件所在目录
     */
    private String path;

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

        File folder = new File(location);
        if (!folder.exists() && !folder.mkdirs())
            throw new ConfigInitializationException("Can not create folder at " + folder.getAbsolutePath());

        if (!location.endsWith("/"))
            location += "/";

        this.path = location;
        location += "config.json";
        this.location = location;

        File connTimeoutFile = new File(this.path, CONNECT_TIMEOUT_FILE);
        if (connTimeoutFile.exists()) {
            if (connTimeoutFile.isDirectory()) {
                throw new ConfigInitializationException("File at " + connTimeoutFile.getAbsolutePath() + " is a Directory!");
            }

            try (FileInputStream is = new FileInputStream(connTimeoutFile);
                 Scanner sc = new Scanner(is)) {
                this.connectTimeout = Integer.parseInt(sc.next());
            } catch (IOException e) {
                throw new ConfigInitializationException("Can not open file at " + connTimeoutFile.getAbsolutePath(), e);
            } catch (NumberFormatException e) {
                this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
                log.warn("Illegal file format at {}, use the default value as connect timeout", connTimeoutFile.getAbsoluteFile());
            }
        } else {
            try {
                makeTemplateConnectTimeFile(connTimeoutFile);
                this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            } catch (IOException e) {
                log.warn("Can not create file at {}, use the default value as connect timeout", connTimeoutFile.getAbsolutePath(), e);
            }
        }


        File guiFile = new File(this.path, GUI_OPTION_FILE);
        if (guiFile.exists()) {
            if (guiFile.isDirectory()) {
                throw new ConfigInitializationException("File at " + guiFile.getAbsolutePath() + " is a Directory!");
            }

            try (FileInputStream is = new FileInputStream(guiFile);
                 Scanner sc = new Scanner(is)) {
                this.openGUI = sc.nextBoolean();
            } catch (IOException e) {
                throw new ConfigInitializationException("Can not open file at " + guiFile.getAbsolutePath(), e);
            } catch (NoSuchElementException e) {
                this.openGUI = true;
                log.warn("Illegal file format at {}, use the default value as gui option", guiFile.getAbsoluteFile());
            }
        } else {
            try {
                makeTemplateGUIOptionFile(guiFile);
                this.openGUI = true;
            } catch (IOException e) {
                throw new ConfigInitializationException("Can not create file at " + guiFile.getAbsolutePath(), e);
            }
        }

    }

    /**
     * @return 用户配置文件的URL
     */
    public String configLocationURL() {
        return "file:///" + location;
    }

    /**
     * @return 用户配置文件的路径
     */
    public String configLocation() {
        return location;
    }

    /**
     * @return 配置文件目录
     */
    public String configPath() {
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
     * @param file 文件路径
     */
    private void makeTemplateConnectTimeFile(File file) throws IOException {
        String content = String.valueOf(DEFAULT_CONNECT_TIMEOUT);
        ByteBuffer buf = ByteBuffer.allocate(content.length());
        buf.put(content.getBytes(StandardCharsets.US_ASCII));
        writeFile(file, buf);
    }

    /**
     * 创建默认GUI设置文件
     *
     * @param file 文件路径
     * @throws IOException 当写入失败
     */
    private void makeTemplateGUIOptionFile(File file) throws IOException {
        String content = "true";
        ByteBuffer buf = ByteBuffer.allocate(content.length());
        buf.put(content.getBytes(StandardCharsets.US_ASCII));
        writeFile(file, buf);
    }

    private void writeFile(File file, ByteBuffer buf) throws IOException {
        try (FileChannel ch = FileChannel.open(file.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            buf.rewind();
            ch.write(buf);
        }
    }
}
