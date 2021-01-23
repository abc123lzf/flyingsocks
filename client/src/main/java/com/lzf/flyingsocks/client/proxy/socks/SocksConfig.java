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
package com.lzf.flyingsocks.client.proxy.socks;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.GlobalConfig;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 本地Socks5代理端口配置信息
 */
public class SocksConfig extends AbstractConfig {

    public static final String NAME = "config.socks";


    private static final String SOCKS_CONFIG_FILE = "socks-setting.json";

    /**
     * 是否需要认证
     */
    private boolean auth;

    /**
     * Socks5代理端口
     */
    private int port;

    /**
     * 用户名，仅在auth为true时有效
     */
    private String username;

    /**
     * 密码，仅在auth为false时有效
     */
    private String password;

    /**
     * Socks5端口绑定地址
     */
    private String address;

    public SocksConfig(ConfigManager<?> configManager) {
        super(configManager, NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        GlobalConfig cfg = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);
        Path path = cfg.configPath().resolve(SOCKS_CONFIG_FILE);
        if (!Files.exists(path)) {
            try {
                makeSocksSettingFile(path);
            } catch (IOException e) {
                throw new ConfigInitializationException("Create new file at " + path + " occur a exception", e);
            }
        } else if (Files.isDirectory(path)) {
            throw new ConfigInitializationException("location at " + path + " is a Directory");
        }

        try (FileInputStream is = new FileInputStream(path.toFile())) {
            byte[] b = new byte[(int) Files.size(path)];
            int cnt = is.read(b);
            String json = new String(b, 0, cnt, StandardCharsets.UTF_8);
            JSONObject obj = JSON.parseObject(json);
            this.port = obj.getIntValue("port");
            this.auth = obj.getBooleanValue("auth");
            this.address = obj.getString("address");

            if (address == null) {
                address = "0.0.0.0";
            }

            if (auth) {
                this.username = obj.getString("username");
                this.password = obj.getString("password");
                if (username == null) {
                    throw new ConfigInitializationException("username should not be null");
                }
            }
        } catch (IOException | JSONException | NumberFormatException e) {
            throw new ConfigInitializationException("Exception occur when reading socks-setting file at" + path, e);
        }
    }

    private static void makeSocksSettingFile(Path path) throws IOException {
        JSONObject socks = new JSONObject();
        socks.put("address", "127.0.0.1");
        socks.put("port", 1080);
        socks.put("auth", false);

        String content = socks.toJSONString();
        ByteBuffer buf = ByteBuffer.allocate(content.length());
        buf.put(content.getBytes(StandardCharsets.US_ASCII));
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            buf.rewind();
            ch.write(buf);
        }
    }

    @Override
    public boolean canSave() {
        return true;
    }

    @Override
    public void save() throws Exception {
        GlobalConfig cfg = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);

        Path f = cfg.configPath().resolve(SOCKS_CONFIG_FILE);
        JSONObject socks = new JSONObject();
        socks.put("address", address);
        socks.put("port", port);
        socks.put("auth", auth);
        if (auth) {
            socks.put("username", username);
            socks.put("password", password);
        }

        try (FileWriter writer = new FileWriter(f.toFile())) {
            writer.write(socks.toString(SerializerFeature.PrettyFormat));
        }
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

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return address;
    }

    public void update(int port, boolean auth, String username, String password) {
        if (port > 0)
            this.port = port;

        if (this.auth != auth) {
            if (auth) {
                this.username = username;
                this.password = password;
                this.auth = auth;
            } else {
                this.auth = auth;
                this.username = username;
                this.password = password;
            }
        }

        configManager.updateConfig(this);
    }


    public SocksConfig configFacade() {
        return new Facade(configManager, this);
    }


    static class Facade extends SocksConfig {
        Facade(ConfigManager<?> configManager, SocksConfig config) {
            super(configManager);
            super.auth = config.auth;
            super.address = config.address;
            super.username = config.username;
            super.password = config.password;
            super.port = config.port;
        }

        @Override
        public void save() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(int port, boolean auth, String username, String password) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SocksConfig configFacade() {
            throw new UnsupportedOperationException();
        }
    }
}
