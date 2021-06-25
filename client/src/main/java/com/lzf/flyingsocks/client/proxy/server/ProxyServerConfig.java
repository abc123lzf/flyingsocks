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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProxyServerConfig extends AbstractConfig {

    private static final Logger log = LoggerFactory.getLogger(ProxyServerConfig.class);

    public static final String DEFAULT_NAME = "config.proxyserver";

    private static final String SERVER_SETTING_FILE = "server-setting.json";

    /**
     * 服务器节点集合
     */
    private final Map<String, Node> servers = new ConcurrentHashMap<>();

    /**
     * 服务器组
     */
    private final Map<String, Group> groups = new ConcurrentHashMap<>();


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

            JSONObject object = JSON.parseObject(new String(b, StandardCharsets.UTF_8));

            JSONArray servers = object.getJSONArray("servers");
            for (int i = 0; i < servers.size(); i++) {
                JSONObject o = servers.getJSONObject(i);
                String id = o.getString("id");
                String host = o.getString("host");
                int port = o.getIntValue("port");
                int certPort = o.getIntValue("cert-port");
                AuthType type = AuthType.valueOf(o.getString("auth").toUpperCase());

                Map<String, String> authArg = new HashMap<>(4);
                JSONObject auth = o.getJSONObject("auth-arg");
                auth.forEach((k, v) -> authArg.put(k, v.toString()));

                boolean use = o.getBooleanValue("state");
                EncryptType etype = EncryptType.valueOf(o.getString("encrypt").toUpperCase());

                this.servers.putIfAbsent(id, new Node(id, host, port, certPort, type, etype, authArg, use));
            }

            JSONArray groups = object.getJSONArray("groups");
            for (int i = 0; i < groups.size(); i++) {
                JSONObject o = groups.getJSONObject(i);
                String id = o.getString("id");
                String name = o.getString("name");
                JSONArray nodes = o.getJSONArray("nodes");
                List<Node> groupNodes = new ArrayList<>(nodes.size());
                for (int j = 0; j < nodes.size(); j++) {
                    String nid = nodes.getString(i);
                    Node n = this.servers.get(nid);
                    if (n == null) {
                        log.warn("Server configuration id [{}] not found, ignore", nid);
                        continue;
                    }
                    groupNodes.add(n);
                }

                Group group = new Group(id, name);
                group.addNodes(groupNodes);
                this.groups.put(id, group);
            }

        } catch (IOException e) {
            throw new ConfigInitializationException(e);
        } catch (RuntimeException e) {
            log.warn("Load config at {} failure", path, e);
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

        JSONObject object = new JSONObject();
        JSONArray servers = new JSONArray(this.servers.size());
        for (Node node : this.servers.values()) {
            JSONObject o = new JSONObject();
            o.put("id", node.getId());
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
            servers.add(o);
        }

        object.put("servers", servers);

        JSONArray groups = new JSONArray(this.groups.size());
        

        try (FileWriter writer = new FileWriter(p.toFile())) {
            writer.write(object.toString(SerializerFeature.PrettyFormat));
        }
    }

    public Node[] getProxyServerConfig() {
        return this.servers.values().toArray(new Node[0]);
    }


    public void addProxyServerNode(Node node) {
        if (node.getId() == null) {
            node.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        servers.put(node.getId(), node);
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
        return this.servers.containsKey(node.getId());
    }

    public void setProxyServerUsing(Node node, boolean use) {
        if (!servers.containsKey(node.getId()))
            throw new IllegalArgumentException(String.format("Server Node %s:%d not exists", node.getHost(), node.getPort()));
        if (node.isUse() == use) {
            return;
        }
        node.setUse(use);
        configManager.updateConfig(this);
    }

    public void setProxyServerUsing(Map<Node, Boolean> map) {
        map.forEach((node, using) -> {
            if (!this.servers.containsKey(node.getId())) {
                throw new IllegalArgumentException(String.format("Server Node %s:%d not exists", node.getHost(), node.getPort()));
            }

            if (node.isUse() != using) {
                node.setUse(using);
            }
        });

        configManager.updateConfig(this);
    }


    public void removeProxyServerNode(Node node) {
        this.servers.remove(node.getId());
        configManager.updateConfig(this);
    }


    public enum AuthType {
        SIMPLE, USER
    }


    public enum EncryptType {
        NONE, SSL, SSL_CA
    }


    public static final class Node {
        private String id;
        private String host;
        private int port;
        private int certPort;
        private AuthType authType;
        private Map<String, String> authArgument;
        private EncryptType encryptType;

        private boolean use;

        public Node() {
        }

        public Node(String id, String host, int port, int certPort, AuthType authType, EncryptType type,
                    Map<String, String> authArgument, boolean use) {
            this.id = id;
            this.host = host;
            this.port = port;
            this.certPort = certPort;
            this.authType = authType;
            this.authArgument = authArgument;
            this.use = use;
            this.encryptType = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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


    public static final class Group {
        private String id;
        private String name;
        private final List<Node> nodes = new CopyOnWriteArrayList<>();

        public Group() {
        }

        public Group(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Node> getNodes() {
            return Collections.unmodifiableList(nodes);
        }

        public void addNodes(Collection<Node> nodes) {
            this.nodes.addAll(nodes);
        }
    }
}
