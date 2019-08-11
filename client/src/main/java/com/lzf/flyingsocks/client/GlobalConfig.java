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

/**
 * 初始配置文件，用于获取用户配置文件路径、GUI设置以及应用程序超时时间
 */
public class GlobalConfig extends AbstractConfig  {

    public static final String NAME = "config.global";

    private static final String PATH = "classpath://config.properties";

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
    private int connectionTimeout;


    GlobalConfig(ConfigManager<?> configManager) {
        super(configManager, NAME);
    }

    /**
     * 加载基本配置文件，并初始化用户存档文件
     * @throws ConfigInitializationException 如果无法获取基本配置文件或者无法创建用户存档文件
     */
    @Override
    protected void initInternal() throws ConfigInitializationException {
        //加载基本配置文件
        try(InputStream is = configManager.loadResource(PATH)) {
            Properties p = new Properties();
            p.load(is);
            String location;
            if(configManager.isWindows()) {
                location = p.getProperty("config.location.windows");
            } else {
                location = p.getProperty("config.location.linux");
            }

            File folder = new File(location);
            if(!folder.exists() && !folder.mkdirs())
                throw new ConfigInitializationException("Can not create folder at " + folder.getAbsolutePath());

            if(!location.endsWith("/"))
                location += "/";

            this.path = location;
            location += "config.json";

            File file = new File(location);
            if(!file.exists()) {  //如果用户配置文件不存在，则初始化用户配置
                makeTemplateConfigFile(file);
            }

            //加载用户配置文件
            try(InputStream cis = file.toURI().toURL().openStream()) {
                byte[] b = new byte[512000];
                int len = cis.read(b);
                String json = new String(b, 0, len, Charset.forName("UTF-8"));

                JSONObject obj = JSON.parseObject(json);
                this.openGUI = obj.getBooleanValue("gui");
                this.connectionTimeout = obj.getIntValue("connect-timeout");
            }

            this.location = location;

        } catch (IOException e) {
            throw new ConfigInitializationException(e);
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
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * 生成一个默认配置文件
     * @param file 文件路径
     * @throws IOException 如果路径不存在
     */
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
