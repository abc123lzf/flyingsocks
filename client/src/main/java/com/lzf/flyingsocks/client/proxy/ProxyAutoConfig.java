package com.lzf.flyingsocks.client.proxy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.GlobalConfig;
import com.lzf.flyingsocks.util.BaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PAC模式配置
 */
public class ProxyAutoConfig extends AbstractConfig implements Config {
    public static final String DEFAULT_NAME = "Config-PAC";

    private static final Logger log = LoggerFactory.getLogger(ProxyAutoConfig.class);

    private static final String DEFAULT_PAC_CONFIG_LOCATION = "classpath://pac.txt";
    private static final Charset DEFAULT_CONFIG_ENCODING = Charset.forName("UTF-8");

    private static final String PAC_CONFIG_FILE = "pac-setting";

    public static final int PROXY_NO = 0;
    public static final int PROXY_PAC = 1;
    public static final int PROXY_GLOBAL = 2;

    /**
     * 系统代理模式
     */
    private int proxyMode;

    /**
     * 需要代理的域名/IP列表
     */
    private final Set<String> proxySet = Collections.newSetFromMap(new ConcurrentHashMap<>(2048));

    ProxyAutoConfig(ConfigManager<?> configManager) {
        super(configManager, DEFAULT_NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        try(InputStream is = configManager.loadResource(DEFAULT_PAC_CONFIG_LOCATION)) {
            loadDefaultPacFile(is);
        } catch (IOException e) {
            throw new ConfigInitializationException(e);
        }

        GlobalConfig cfg = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);
        File file = new File(cfg.configPath(), PAC_CONFIG_FILE);

        if(!file.exists()) {
            try {
                this.proxyMode = PROXY_PAC;
                makeTemplatePACFile(file);
            } catch (IOException e) {
                throw new ConfigInitializationException("Can not create new File at " + file.getAbsolutePath());
            }
        } else {
            if(file.isDirectory()) {
                throw new ConfigInitializationException("File at " + file.getAbsolutePath() + " is a Directory!");
            }

            try (FileInputStream is = new FileInputStream(file);
                 Scanner sc = new Scanner(is)) {
                String s = sc.next();
                switch (s) {
                    case "no": this.proxyMode = PROXY_NO; break;
                    case "pac": this.proxyMode = PROXY_PAC; break;
                    case "global": this.proxyMode = PROXY_GLOBAL; break;
                    default: {
                        log.warn("Config file pac setting is not correct, only 'no' / 'pac' / 'global'");
                        this.proxyMode = PROXY_PAC;
                    }
                }
            } catch (IOException e) {
                throw new ConfigInitializationException();
            }
        }
    }

    /**
     * 加载默认的PAC文件
     * @param is 文件输入流
     * @throws IOException 加载错误
     */
    private void loadDefaultPacFile(InputStream is) throws IOException {
        byte[] b = new byte[5120000];
        int r = is.read(b);

        String str = new String(b, 0, r, DEFAULT_CONFIG_ENCODING);
        JSONObject object;
        try {
            object = JSON.parseObject(str);
        } catch (JSONException e) {
            if(log.isErrorEnabled())
                log.error("PAC file format is not illegal.");
            throw new ConfigInitializationException(e);
        }

        object.forEach((host, enabled) -> {
            if(object.getInteger(host) != 0)
                proxySet.add(host);
        });
    }


    @Override
    public boolean canSave() {
        return true;
    }

    @Override
    public void save() throws Exception {
        GlobalConfig cfg = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);

        File f = new File(cfg.configPath(), PAC_CONFIG_FILE);
        String content;
        switch (this.proxyMode) {
            case PROXY_NO: content = "no"; break;
            case PROXY_PAC: content = "pac"; break;
            case PROXY_GLOBAL: content = "global"; break;
            default:
                content = "";
        }

        try(FileWriter writer = new FileWriter(f)) {
            writer.write(content);
        }
    }

    public int getProxyMode() {
        return proxyMode;
    }


    public void setProxyMode(int proxyMode) {
        if(this.proxyMode == proxyMode)
            return;

        if(proxyMode < 0 || proxyMode > 2)
            throw new IllegalArgumentException();

        this.proxyMode = proxyMode;
        configManager.updateConfig(this);
    }

    boolean needProxy(String host) {
        if(proxyMode == PROXY_NO)
            return false;
        if(proxyMode == PROXY_GLOBAL)
            return true;
        if(proxyMode == PROXY_PAC) {
            String[] strings = BaseUtils.splitPreserveAllTokens(host, '.');
            int len = strings.length;
            if (len <= 1)
                return false;
            String realHost = strings[len - 2] + "." + strings[len - 1];
            return proxySet.contains(realHost);
        } else {
            throw new IllegalStateException("ProxyMode is not correct");
        }
    }

    private void makeTemplatePACFile(File file) throws IOException {
        String content = "pac";
        ByteBuffer buf = ByteBuffer.allocate(content.length());
        buf.put(content.getBytes(Charset.forName("ASCII")));
        try(FileChannel ch = FileChannel.open(file.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            buf.rewind();
            ch.write(buf);
        }
    }
}
