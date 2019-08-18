package com.lzf.flyingsocks.client.proxy.socks;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.GlobalConfig;

import java.io.*;
import java.nio.charset.Charset;

/**
 * 本地Socks5代理端口配置信息
 */
public class SocksConfig extends AbstractConfig {

    public static final String NAME = "config.socks";

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

    SocksConfig(ConfigManager<?> configManager) {
        super(configManager, NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        GlobalConfig cfg = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);
        String url = cfg.configLocationURL();

        try(InputStream is = configManager.loadResource(url)) {
            byte[] b = new byte[512000];
            int len = is.read(b);
            String json = new String(b, 0, len, Charset.forName("UTF-8"));
            JSONObject obj = JSON.parseObject(json).getJSONObject("socks");
            this.port = obj.getIntValue("port");
            this.auth = obj.getBooleanValue("auth");
            this.address = obj.getString("address");

            if(address == null)
                address = "0.0.0.0";

            if(auth) {
                this.username = obj.getString("username");
                this.password = obj.getString("password");
                if(username == null)
                    throw new ConfigInitializationException("When socks auth is true, the username should not be null");
            }

        } catch (IOException | JSONException | NumberFormatException e) {
            throw new ConfigInitializationException(e);
        }
    }

    @Override
    public boolean canSave() {
        return true;
    }

    @Override
    public void save() throws Exception {
        GlobalConfig cfg = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);

        File f = new File(cfg.configLocation());
        JSONObject obj;
        if(f.exists() && f.length() > 0) {
            FileReader reader = new FileReader(f);
            char[] s = new char[(int)f.length()];
            int r = reader.read(s);
            if(r < f.length()) {
                char[] os = s;
                s = new char[r];
                System.arraycopy(os, 0, s, 0, r);
            }
            obj = JSON.parseObject(new String(s));
            reader.close();
        } else {
            obj = new JSONObject();
        }

        JSONObject socks = new JSONObject();
        socks.put("address", address);
        socks.put("port", port);
        socks.put("auth", auth);
        if(auth) {
            socks.put("username", username);
            socks.put("password", password);
        }

        obj.put("socks", socks);

        FileWriter writer = new FileWriter(f);
        writer.write(obj.toJSONString());
        writer.close();
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

    public void update(boolean auth, String username, String password) {
        this.username = username;
        this.password = password;
        this.auth = auth;
        configManager.updateConfig(this);
    }
}
