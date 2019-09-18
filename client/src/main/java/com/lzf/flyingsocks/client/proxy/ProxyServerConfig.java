package com.lzf.flyingsocks.client.proxy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.GlobalConfig;
import com.lzf.flyingsocks.protocol.AuthMessage;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProxyServerConfig extends AbstractConfig {

    public static final String DEFAULT_NAME = "config.proxyserver";

    private static final String SERVER_SETTING_FILE = "server-setting.json";

    /**
     * 服务器节点集合
     */
    private final List<Node> nodes = new CopyOnWriteArrayList<>();


    ProxyServerConfig(ConfigManager<?> configManager) {
        super(configManager, DEFAULT_NAME);
    }


    @Override
    protected void initInternal() throws ConfigInitializationException {
        GlobalConfig cfg = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);
        File file = new File(cfg.configPath(), SERVER_SETTING_FILE);

        if(!file.exists()) {
            return;
        }

        try(FileInputStream is = new FileInputStream(file)) {
            byte[] b = new byte[(int)file.length()];
            is.read(b);
            JSONArray array = JSON.parseArray(new String(b, Charset.forName("UTF-8")));

            for(int i = 0; i < array.size(); i++) {
                JSONObject o = array.getJSONObject(i);
                String host = o.getString("host");
                int port = o.getIntValue("port");
                int certPort = o.getIntValue("cert-port");
                String t;
                AuthType type = AuthType.valueOf(t = o.getString("auth").toUpperCase());

                List<String> keys = AuthMessage.AuthMethod.valueOf(t).getContainsKey();

                Map<String, String> authArg = new HashMap<>(4);
                JSONObject auth = o.getJSONObject("auth-arg");
                for(String key : keys) {
                    String v = auth.getString(key);
                    if(v == null)
                        throw new ConfigInitializationException(String.format("config.json: node %s:%d auth-arg need key %s",
                                host, port, key));
                    authArg.put(key, v);
                }

                boolean use = o.getBooleanValue("state");
                EncryptType etype = EncryptType.valueOf(o.getString("encrypt").toUpperCase());

                nodes.add(new Node(host, port, certPort, type, etype, authArg, use));
            }
        } catch (IOException e) {
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

        File f = new File(cfg.configPath(), SERVER_SETTING_FILE);

        JSONArray arr = new JSONArray(nodes.size());
        for(Node node : nodes) {
            JSONObject o = new JSONObject();
            o.put("host", node.getHost());
            o.put("port", node.getPort());
            o.put("cert-port", node.getCertPort());
            o.put("auth", node.getAuthType().name().toLowerCase());
            JSONObject auth = new JSONObject();
            auth.putAll(node.authArgument);
            o.put("auth-arg", auth);
            o.put("state", node.isUse());
            o.put("encrypt", node.encryptType.name());
            arr.add(o);
        }

        try(FileWriter writer = new FileWriter(f)) {
            writer.write(arr.toJSONString());
        }
    }

    public Node[] getProxyServerConfig() {
        return nodes.toArray(new Node[0]);
    }


    public void addProxyServerNode(Node node) {
        nodes.add(node);
        configManager.updateConfig(this);
    }

    public void updateProxyServerNode(Node node) {
        if(containsProxyServerNode(node))
            configManager.updateConfig(this);
        else
            throw new IllegalArgumentException(String.format("Node %s:%d doesn't exists",
                    node.getHost(), node.getPort()));
    }

    public boolean containsProxyServerNode(Node node) {
        return nodes.contains(node);
    }

    public void setProxyServerUsing(Node node, boolean use) {
        if(!nodes.contains(node))
            throw new IllegalArgumentException(String.format("Server Node %s:%d not exists", node.getHost(), node.getPort()));
        if(node.isUse() == use)
            return;
        node.setUse(use);
        configManager.updateConfig(this);
    }

    public void setProxyServerUsing(Map<Node, Boolean> map) {
        map.forEach((node, using) -> {
            if(!nodes.contains(node)) {
                throw new IllegalArgumentException(String.format("Server Node %s:%d not exists", node.getHost(), node.getPort()));
            }

            if(node.isUse() != using) {
                node.setUse(using);
            }
        });

        configManager.updateConfig(this);
    }


    public void removeProxyServerNode(Node node) {
        nodes.remove(node);
        configManager.updateConfig(this);
    }


    public enum AuthType {
        SIMPLE, USER
    }


    public enum EncryptType {
        NONE, SSL
    }


    public static final class Node {
        private String host;
        private int port;
        private int certPort;
        private AuthType authType;
        private Map<String, String> authArgument;
        private EncryptType encryptType;

        private boolean use;

        public Node() { }

        public Node(String host, int port, int certPort, AuthType authType, EncryptType type,
                    Map<String, String> authArgument, boolean use) {
            this.host = host;
            this.port = port;
            this.certPort = certPort;
            this.authType = authType;
            this.authArgument = authArgument;
            this.use = use;
            this.encryptType = type;
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

        public int getCertPort() {
            return certPort;
        }

        public void setCertPort(int certPort) {
            this.certPort = certPort;
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
            if(authArgument == null)
                authArgument = new HashMap<>(4);
            authArgument.put(key, val);
        }

        public void setAuthArgument(Map<String, String> arg) {
            this.authArgument = arg;
        }

        public boolean isUse() {
            return use;
        }

        private void setUse(boolean use) {
            this.use = use;
        }

        public EncryptType getEncryptType() {
            return encryptType;
        }

        public void setEncryptType(EncryptType encryptType) {
            this.encryptType = encryptType;
        }
    }
}
