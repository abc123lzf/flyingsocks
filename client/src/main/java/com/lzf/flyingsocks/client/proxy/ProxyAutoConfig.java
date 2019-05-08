package com.lzf.flyingsocks.client.proxy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PAC模式配置
 */
public class ProxyAutoConfig extends AbstractConfig implements Config {
    public static final String DEFAULT_NAME = "Config-PAC";

    private static final Logger log = LoggerFactory.getLogger(ProxyAutoConfig.class);

    private static final String DEFAULT_PAC_CONFIG_LOCATION = "classpath://pac.txt";
    private static final String DEFAULT_PAC_CONFIG_ENCODING = "UTF-8";

    private static final int PROXY_NO = 0;
    private static final int PROXY_PAC = 1;
    private static final int PROXY_GLOBAL = 2;

    private int proxyMode = PROXY_GLOBAL;

    private final Set<String> proxySet = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ProxyAutoConfig(ConfigManager<?> configManager) {
        super(configManager, DEFAULT_NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        InputStream is;
        try {
            is = configManager.loadResource(DEFAULT_PAC_CONFIG_LOCATION);
        } catch (IOException e) {
            throw new ConfigInitializationException(e);
        }
        byte[] b = new byte[5120000];
        int r;
        try {
            r = is.read(b);
        } catch (IOException e) {
            if(log.isErrorEnabled())
                log.error("This PAC file is too big, this file should be 5MB maximum.");
            throw new ConfigInitializationException(e);
        }

        String str = new String(b, 0, r, Charset.forName(DEFAULT_PAC_CONFIG_ENCODING));
        JSONObject object;
        try {
            object = JSON.parseObject(str);
        } catch (JSONException e) {
            if(log.isErrorEnabled())
                log.error("PAC file format is not illegal.");
            throw new ConfigInitializationException(e);
        }

        for(Map.Entry<String, Object> entry : object.entrySet()) {
            String k;
            if(object.getInteger(k = entry.getKey()) != 0)
                proxySet.add(k);
        }
    }

    public boolean needProxy(String host) {
        if(proxyMode == PROXY_NO)
            return false;
        if(proxyMode == PROXY_GLOBAL)
            return true;
        if(proxyMode == PROXY_PAC) {
            String[] strings = host.split("\\.");
            int len = strings.length;
            if (len <= 1)
                return false;
            String realHost = strings[len - 2] + "." + strings[len - 1];
            return proxySet.contains(realHost);
        } else {
            throw new IllegalStateException("ProxyMode is not correct");
        }
    }
}
