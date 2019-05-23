package com.lzf.flyingsocks.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class GlobalConfig extends AbstractConfig  {
    public static final String NAME = "config.global";
    private static final String PATH = "classpath://config.json";

    private boolean openGUI;

    private int connectionTimeout;

    public GlobalConfig(ConfigManager<?> configManager) {
        super(configManager, NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        try(InputStream is = configManager.loadResource(PATH)) {
            byte[] b = new byte[512000];
            int len = is.read(b);
            String json = new String(b, 0, len, Charset.forName("UTF-8"));

            JSONObject obj = JSON.parseObject(json);
            openGUI = obj.getBooleanValue("gui");
            connectionTimeout = obj.getIntValue("connect-timeout");

        } catch (IOException e) {
            throw new ConfigInitializationException(e);
        }
    }

    public boolean isOpenGUI() {
        return openGUI;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }
}
