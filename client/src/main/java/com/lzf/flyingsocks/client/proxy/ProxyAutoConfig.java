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
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * PAC模式配置
 */
public class ProxyAutoConfig extends AbstractConfig implements Config {
    public static final String DEFAULT_NAME = "Config-PAC";

    private static final Logger log = LoggerFactory.getLogger(ProxyAutoConfig.class);

    private static final Charset DEFAULT_CONFIG_ENCODING = StandardCharsets.UTF_8;

    private static final String PAC_CONFIG_FILE = "pac-setting";
    private static final String GFWLIST_FILE = "gfwlist.txt";

    public static final int PROXY_NO = 0;
    public static final int PROXY_GFW_LIST = 1;
    public static final int PROXY_GLOBAL = 2;
    public static final int PROXY_NON_CN = 3;


    /**
     * 系统代理模式
     */
    private volatile int proxyMode;

    /**
     * 需要代理的域名/IP列表,仅PAC模式使用
     */
    private Set<String> proxySet = Collections.emptySet();

    /**
     * 中国IP地址Map
     */
    private Map<Integer, Integer> chinaIpv4Map = Collections.emptyMap();


    ProxyAutoConfig(ConfigManager<?> configManager) {
        super(configManager, DEFAULT_NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        GlobalConfig cfg = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);
        File gfwFile = new File(cfg.configPath(), GFWLIST_FILE);
        if(!gfwFile.exists()) {
            copyGFWListConfig();
        }

        try {
            loadDefaultPacFile(gfwFile);
        } catch (IOException e) {
            log.error("Read GFWList file occur a exception", e);
            System.exit(1);
        }


        File file = new File(cfg.configPath(), PAC_CONFIG_FILE);
        if(!file.exists()) {
            try {
                this.proxyMode = PROXY_GFW_LIST;
                makeTemplatePACSettingFile(file);
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
                    case "pac": this.proxyMode = PROXY_GFW_LIST; break;
                    case "global": this.proxyMode = PROXY_GLOBAL; break;
                    default: {
                        log.warn("Config file pac setting is not correct, only 'no' / 'pac' / 'global'");
                        this.proxyMode = PROXY_GFW_LIST;
                    }
                }
            } catch (IOException e) {
                throw new ConfigInitializationException();
            }
        }
    }

    /**
     * 加载默认的GFWList文件
     * @param f GFWList文件路径
     * @throws IOException 加载错误
     */
    private void loadDefaultPacFile(File f) throws IOException {
        byte[] b = new byte[(int)f.length()];
        try(FileInputStream fis = new FileInputStream(f)) {
            fis.read(b);
        }

        String str = new String(b, 0, b.length, DEFAULT_CONFIG_ENCODING);
        JSONObject object = null;
        try {
            object = JSON.parseObject(str);
        } catch (JSONException e) {
            log.error("GFWList file format is not illegal.", e);
            System.exit(1);
        }

        Set<String> set = new HashSet<>(8192);
        object.forEach((host, enabled) -> {
            if((int)enabled != 0)
                set.add(host);
        });

        proxySet = Collections.unmodifiableSet(set);
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
            case PROXY_GFW_LIST: content = "pac"; break;
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
        if(proxyMode == PROXY_GFW_LIST) {
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

    private void makeTemplatePACSettingFile(File file) throws IOException {
        String content = "pac";
        ByteBuffer buf = ByteBuffer.allocate(content.length());
        buf.put(content.getBytes(StandardCharsets.US_ASCII));
        try(FileChannel ch = FileChannel.open(file.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            buf.rewind();
            ch.write(buf);
        }
    }

    private void copyGFWListConfig() {
        log.info("Can not found GFWList file on User DIR, ready to copy default file to User DIR.");

        byte[] b = new byte[5120000];
        try(InputStream is = configManager.loadResource(configManager.getSystemProperties("pac.gfwlist.config.url"))) {
            int r = is.read(b);
            String str = new String(b, 0, r, DEFAULT_CONFIG_ENCODING);
            GlobalConfig cfg = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);
            File f = new File(cfg.configPath(), GFWLIST_FILE);
            try(FileWriter fw = new FileWriter(f)) {
                fw.write(str);
            }
        } catch (IOException e) {
            log.error("Can not find default pac file", e);
            System.exit(1);
        }
    }
}
