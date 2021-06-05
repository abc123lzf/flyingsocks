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
package com.lzf.flyingsocks.client.proxy.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.GlobalConfig;
import com.lzf.flyingsocks.misc.BaseUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * HTTP代理配置
 *
 * @author lzf abc123lzf@126.com
 * @since 2020/12/26 3:03
 */
public class HttpProxyConfig extends AbstractConfig {

    public static final String NAME = "config.httpproxy";

    private static final String HTTP_CONFIG_FILE = "http-setting.json";

    /**
     * 配置文件路径
     */
    private Path filePath;

    /**
     * 服务绑定地址，默认127.0.0.1
     */
    private String address;

    /**
     * 绑定的本地端口，默认8080
     */
    private int port;

    /**
     * 是否使用简易认证
     */
    private volatile boolean auth;

    /**
     * 简易认证用户名
     */
    private String username;

    /**
     * 简易认证密码
     */
    private String password;

    /**
     * 是否打开系统代理
     */
    private boolean enableWindowsSystemProxy;


    public HttpProxyConfig(ConfigManager<?> configManager) {
        super(Objects.requireNonNull(configManager), NAME);
    }


    @Override
    protected void initInternal() throws ConfigInitializationException {
        Path path = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class).configPath();
        Path config = path.resolve(HTTP_CONFIG_FILE);
        this.filePath = config;
        createFileIfNotExists(config);
        loadConfigFile(config);
    }


    public String getBindAddress() {
        return address;
    }

    public int getBindPort() {
        return port;
    }

    public boolean isAuth() {
        return auth;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isEnableWindowsSystemProxy() {
        return enableWindowsSystemProxy;
    }

    private void loadConfigFile(Path path) {
        JSONObject json;
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            int size = (int) Files.size(path);
            ByteBuffer buffer = ByteBuffer.allocate(size);
            ch.read(buffer);
            buffer.flip();
            json = JSON.parseObject(new String(buffer.array(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            throw new ConfigInitializationException("Load http-setting.json failure", e);
        }

        boolean auth = json.getBooleanValue("auth");
        int port = json.getIntValue("port");
        if (!BaseUtils.isPort(port)) {
            throw new ConfigInitializationException("[http-setting.json] port: " + port);
        }

        String address = json.getString("address");
        if (address == null) {
            address = "127.0.0.1";
        }

        this.port = port;
        this.address = address;

        if (auth) {
            String username = json.getString("username");
            String password = json.getString("password");

            if (!StringUtils.isAnyBlank(username, password)) {
                this.auth = auth;
                this.username = username;
                this.password = password;
            } else {
                this.auth = false;
            }
        }

        this.enableWindowsSystemProxy = configManager.isWindows() && json.getBooleanValue("windowsSystemProxy");
    }


    private static void createFileIfNotExists(Path path) {
        if (Files.exists(path)) {
            return;
        }

        if (Files.isDirectory(path)) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new ConfigInitializationException("Can not create file:" + path, e);
            }
        }

        JSONObject json = new JSONObject();
        json.put("address", "127.0.0.1");
        json.put("port", 8080);
        json.put("auth", false);
        json.put("windowsSystemProxy", false);

        String content = json.toJSONString();
        ByteBuffer buf = ByteBuffer.allocate(content.length());
        buf.put(content.getBytes(StandardCharsets.US_ASCII));
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            buf.rewind();
            ch.write(buf);
        } catch (IOException e) {
            throw new ConfigInitializationException("Can not create file:" + path, e);
        }
    }


    public boolean supportWindowsSystemProxy() {
        return WindowsSystemProxy.isAvailable();
    }


    public void update(int port, boolean auth, String username, String password) {
        this.port = port;
        this.auth = auth;
        this.username = username;
        this.password = password;

        configManager.updateConfig(this);
    }


    public void enableWindowsSystemProxy(boolean open) {
        if (this.enableWindowsSystemProxy == open || (open && !supportWindowsSystemProxy())) {
            return;
        }
        this.enableWindowsSystemProxy = open;
        configManager.updateConfig(this);
    }


    @Override
    public boolean canSave() {
        return true;
    }

    @Override
    public void save() throws Exception {
        JSONObject json = new JSONObject();
        json.put("address", this.address);
        json.put("port", this.port);
        json.put("auth", this.auth);
        json.put("username", this.username);
        json.put("password", this.password);
        json.put("windowsSystemProxy", this.enableWindowsSystemProxy);

        String content = json.toJSONString();
        ByteBuffer buf = ByteBuffer.allocate(content.length());
        buf.put(content.getBytes(StandardCharsets.US_ASCII));
        Path path = this.filePath;
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.WRITE)) {
            buf.rewind();
            ch.write(buf);
        } catch (IOException e) {
            throw new ConfigInitializationException("Can not create file:" + path, e);
        }
    }


    private static class Facade extends HttpProxyConfig {

        private Facade(HttpProxyConfig config) {
            super(config.configManager);
            super.filePath = config.filePath;
            super.address = config.getBindAddress();
            super.port = config.getBindPort();
            super.auth = config.isAuth();
            super.username = config.getUsername();
            super.password = config.getPassword();
            super.enableWindowsSystemProxy = config.isEnableWindowsSystemProxy();
        }

        @Override
        public boolean canSave() {
            return false;
        }

        @Override
        public void save() throws Exception {
            throw new UnsupportedOperationException("Facade object");
        }

        @Override
        public void update(int port, boolean auth, String username, String password) {
            throw new UnsupportedOperationException("Facade object");
        }

        @Override
        public HttpProxyConfig configFacade() {
            throw new UnsupportedOperationException();
        }
    }


    public HttpProxyConfig configFacade() {
        return new Facade(this);
    }

}
