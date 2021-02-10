/*
 * Copyright (c) 2019 abc123lzf <abc123lzf@126.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lzf.flyingsocks.client.proxy.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.client.GlobalConfig;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
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


    public ProxyServerConfig(ConfigManager<?> configManager) {
        super(configManager, DEFAULT_NAME);
    }


    @Override
    protected void initInternal() throws ConfigInitializationException {
        GlobalConfig cfg = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);
        Path path = cfg.configPath().resolve(SERVER_SETTING_FILE);

        if (!Files.exists(path)) {
            return;
        }

        try (FileInputStream is = new FileInputStream(path.toFile())) {
            byte[] b = new byte[(int) Files.size(path)];
            int cnt = is.read(b);
            if (cnt != Files.size(path)) {
                throw new ConfigInitializationException("File size is abnormalcy, you can restart this application");
            }

            JSONArray array = JSON.parseArray(new String(b, StandardCharsets.UTF_8));

            for (int i = 0; i < array.size(); i++) {
                JSONObject o = array.getJSONObject(i);
                String host = o.getString("host");
                int port = o.getIntValue("port");
                int certPort = o.getIntValue("cert-port");
                AuthType type = AuthType.valueOf(o.getString("auth").toUpperCase());

                Map<String, String> authArg = new HashMap<>(4);
                JSONObject auth = o.getJSONObject("auth-arg");
                auth.forEach((k, v) -> authArg.put(k, v.toString()));

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

        Path p = cfg.configPath().resolve(SERVER_SETTING_FILE);

        JSONArray arr = new JSONArray(nodes.size());
        for (Node node : nodes) {
            JSONObject o = new JSONObject();
            o.put("host", node.getHost());
            o.put("port", node.getPort());
            o.put("cert-port", node.getCertPort());
            o.put("auth", node.getAuthType().name().toLowerCase());
            JSONObject auth = new JSONObject();
            auth.putAll(node.authArgument);
            o.put("auth-arg", auth);
            if (cfg.isEnableGUI()) {
                o.put("state", false);
            } else {  //仅当GUI关闭时才可将自连接设为true
                o.put("state", node.isUse());
            }
            o.put("encrypt", node.encryptType.name());
            arr.add(o);
        }

        try (FileWriter writer = new FileWriter(p.toFile())) {
            writer.write(arr.toString(SerializerFeature.PrettyFormat));
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
        if (containsProxyServerNode(node))
            configManager.updateConfig(this);
        else
            throw new IllegalArgumentException(String.format("Node %s:%d doesn't exists",
                    node.getHost(), node.getPort()));
    }

    public boolean containsProxyServerNode(Node node) {
        return nodes.contains(node);
    }

    public void setProxyServerUsing(Node node, boolean use) {
        if (!nodes.contains(node))
            throw new IllegalArgumentException(String.format("Server Node %s:%d not exists", node.getHost(), node.getPort()));
        if (node.isUse() == use)
            return;
        node.setUse(use);
        configManager.updateConfig(this);
    }

    public void setProxyServerUsing(Map<Node, Boolean> map) {
        map.forEach((node, using) -> {
            if (!nodes.contains(node)) {
                throw new IllegalArgumentException(String.format("Server Node %s:%d not exists", node.getHost(), node.getPort()));
            }

            if (node.isUse() != using) {
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
        NONE, SSL, SSL_CA
    }


    public static final class Node {
        private String host;
        private int port;
        private int certPort;
        private AuthType authType;
        private Map<String, String> authArgument;
        private EncryptType encryptType;

        private boolean use;

        public Node() {
        }

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

        public Map<String, String> allAuthArgument() {
            return Collections.unmodifiableMap(authArgument);
        }

        public void putAuthArgument(String key, String val) {
            if (authArgument == null)
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
