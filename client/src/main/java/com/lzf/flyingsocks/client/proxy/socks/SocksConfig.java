package com.lzf.flyingsocks.client.proxy.socks;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class SocksConfig extends AbstractConfig {

    static final String NAME = "config.socks";

    private static final String PATH = "classpath://config.json";

    private boolean auth;

    private int port;

    private String username;

    private String password;

    private String address;

    public SocksConfig(ConfigManager<?> configManager) {
        super(configManager, NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        try(InputStream is = configManager.loadResource(PATH)) {
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
}
