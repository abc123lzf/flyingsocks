package com.lzf.flyingsocks.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Properties;

public class GlobalConfig extends AbstractConfig  {
    public static final String NAME = "config.global";
    private static final String PATH = "classpath://config.properties";

    private String location;

    private boolean openGUI;

    private int connectionTimeout;

    public GlobalConfig(ConfigManager<?> configManager) {
        super(configManager, NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        try(InputStream is = configManager.loadResource(PATH)) {
            Properties p = new Properties();
            p.load(is);
            String os = configManager.getSystemProperties("os.name").toLowerCase();
            boolean windows = os.startsWith("win");
            String location;
            if(windows) {
                location = p.getProperty("config.location.windows");
            } else {
                location = p.getProperty("config.location.linux");
            }

            File folder = new File(location);
            if(!folder.exists())
                folder.mkdirs();

            if(!location.endsWith("/"))
                location += "/";

            location += "config.json";

            File file = new File(location);
            if(!file.exists()) {
                makeTemplateConfigFile(file);
            }


            try(InputStream cis = file.toURI().toURL().openStream()) {
                byte[] b = new byte[512000];
                int len = cis.read(b);
                String json = new String(b, 0, len, Charset.forName("UTF-8"));

                JSONObject obj = JSON.parseObject(json);
                openGUI = obj.getBooleanValue("gui");
                connectionTimeout = obj.getIntValue("connect-timeout");
            }

            this.location = location;

        } catch (IOException e) {
            throw new ConfigInitializationException(e);
        }
    }

    public String configLocationURL() {
        return "file:///" + location;
    }

    public String configLocation() {
        return location;
    }

    public boolean isOpenGUI() {
        return openGUI;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    private void makeTemplateConfigFile(File file) throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("connect-timeout", 10000);
        obj.put("pac", "pac");
        obj.put("gui", true);

        JSONObject socks = new JSONObject();
        socks.put("address", "0.0.0.0");
        socks.put("port", 1080);
        socks.put("auth", false);

        obj.put("socks", socks);
        obj.put("server", new JSONArray(0));

        FileWriter writer = new FileWriter(file);
        writer.write(obj.toJSONString());
        writer.close();
    }
}
