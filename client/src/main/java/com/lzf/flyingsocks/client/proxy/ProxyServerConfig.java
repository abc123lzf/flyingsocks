package com.lzf.flyingsocks.client.proxy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.protocol.AuthMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProxyServerConfig extends AbstractConfig {
    public static final String DEFAULT_NAME = "Config-ProxyServer";

    private static final String DEFAULT_CONFIG_PATH = "classpath://config.json";

    private List<Node> nodes = new CopyOnWriteArrayList<>();

    public ProxyServerConfig(ConfigManager<?> configManager) {
        super(configManager, DEFAULT_NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        InputStream is = null;
        try {
            is = configManager.loadResource(DEFAULT_CONFIG_PATH);
        } catch (IOException e) {
            try {
                if(is != null)
                    is.close();
            } catch (IOException ignore) {
                //IGNORE
            }
            throw new ConfigInitializationException(e);
        }

        if(is == null)
            return;

        byte[] b = new byte[1024 * 500];
        int size;
        try {
            size = is.read(b);
        } catch (IOException e) {
            throw new ConfigInitializationException(e);
        }

        byte[] nb = new byte[size];
        System.arraycopy(b, 0, nb, 0, size);
        JSONObject obj = JSON.parseObject(new String(nb, Charset.forName("UTF-8")));

        JSONArray array = obj.getJSONArray("server");

        for(int i = 0; i < array.size(); i++) {
            JSONObject o = array.getJSONObject(i);
            String host = o.getString("host");
            int port = o.getIntValue("port");
            String t;
            AuthType type = AuthType.valueOf(t = o.getString("auth").toUpperCase());

            List<String> keys = AuthMessage.AuthMethod.valueOf(t).getContainsKey();
            Map<String, String> authArg = new HashMap<>(4);
            JSONObject auth = o.getJSONObject("auth-arg");
            for(String key : keys) {
                String v = auth.getString(key);
                if(v == null)
                    throw new ConfigInitializationException(String.format("config.json: node %s:%d auth-arg need key %s", host, port, key));
                authArg.put(key, v);
            }

            String jksPath = o.getString("jks-path");
            String jksPass = o.getString("jks-pass");

            boolean use = o.getBooleanValue("state");

            nodes.add(new Node(host, port, type, authArg, jksPath, jksPass, use));
        }
    }

    public Node[] getProxyServerConfig() {
        return nodes.toArray(new Node[nodes.size()]);
    }

    public InputStream loadJksFile(Node node) throws IOException {
        return configManager.loadResource(node.getJksPath());
    }

    public enum AuthType {
        SIMPLE, USER;
    }

    public static final class Node {
        private String host;
        private int port;
        private AuthType authType;
        private Map<String, String> authArgument = new HashMap<>(4);
        private String jksPath;
        private String jksPass;

        private boolean use;

        public Node() { }

        public Node(String host, int port, AuthType authType, Map<String, String> authArgument,
                    String jksPath, String jksPass, boolean use) {
            this.host = host;
            this.port = port;
            this.authType = authType;
            this.authArgument = authArgument;
            this.jksPath = jksPath;
            this.jksPass = jksPass;
            this.use = use;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public AuthType getAuthType() {
            return authType;
        }

        public void setAuthType(AuthType authType) {
            this.authType = authType;
        }

        public String getAuthArgument(String key) {
            return authArgument.get(key);
        }

        public void putAuthArgument(String key, String val) {
            authArgument.put(key, val);
        }

        public void setAuthArgument(Map<String, String> arg) {
            this.authArgument = arg;
        }

        public String getJksPath() {
            return jksPath;
        }

        public void setJksPath(String jksPath) {
            this.jksPath = jksPath;
        }

        public String getJksPass() {
            return jksPass;
        }

        public void setJksPass(String jksPass) {
            this.jksPass = jksPass;
        }

        public boolean isUse() {
            return use;
        }

        public void setUse(boolean use) {
            this.use = use;
        }
    }
}
