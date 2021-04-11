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
package com.lzf.flyingsocks.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.Named;
import com.lzf.flyingsocks.misc.BaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ServerConfig extends AbstractConfig implements Config {

    private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);

    private static final String SERVER_CONFIG_NAME = "server.json";

    public static final String NAME = "config.server";

    private final List<Node> nodeList = new ArrayList<>();

    private Path location;

    ServerConfig(ConfigManager<?> configManager) {
        super(configManager, NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        String folderString = configManager.getSystemProperties("flyingsocks.config.location");
        if (folderString == null) {
            folderString = configManager.getSystemProperties("user.home") + File.separatorChar + "flyingsocks-server";
            log.info("Properties flyingsocks.config.location not configure, using path {}", folderString);
        }

        Path folder = Paths.get(folderString);
        if (Files.isRegularFile(folder)) {
            String msg = "Properties flyingsocks.config.location is file";
            log.error(msg);
            throw new ConfigInitializationException(msg);
        } else if (Files.notExists(folder)) {
            String msg = "Properties flyingsocks.config.location not exists";
            log.error(msg);
            throw new ConfigInitializationException(msg);
        }

        this.location = folder;

        Path file = folder.resolve(SERVER_CONFIG_NAME);
        if (Files.notExists(file)) {
            try {
                makeTemplateConfigFile(file);
            } catch (Exception e) {
                throw new ConfigInitializationException(e);
            }
        }

        try (InputStream cis = Files.newInputStream(file, StandardOpenOption.READ)) {
            byte[] b = new byte[(int) Files.size(file)];
            int len = cis.read(b);
            String json = new String(b, 0, len, StandardCharsets.UTF_8);
            JSONArray arr = JSON.parseArray(json);
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.getString("name");
                int port = obj.getIntValue("port");
                int certPort = obj.getIntValue("cert-port");
                int client = obj.getIntValue("max-client");

                if (!BaseUtils.isPort(port)) {
                    log.error("Illegal Port {}, should be large than 0 and smaller than 65536", port);
                    System.exit(1);
                }

                ClientEncryptType encryptType = ClientEncryptType.configValueOf(obj.getString("encrypt"));
                if (encryptType == null) {
                    throw new ConfigInitializationException("Unsupport encrypt type: " + obj.getString("encrypt"));
                }

                ClientAuthType authType = ClientAuthType.configValueOf(obj.getString("auth-type"));
                if (authType == null) {
                    throw new ConfigInitializationException("Unsupport auth type: " + obj.getString("auth-type"));
                }

                if (encryptType == ClientEncryptType.OpenSSL && !BaseUtils.isPort(certPort)) {
                    log.error("Illegal CertPort {}, should be large than 0 and smaller than 65536", certPort);
                    System.exit(1);
                }

                Node n = new Node(name, port, certPort, client, authType, encryptType);

                switch (authType) {
                    case SIMPLE: {
                        String pwd = obj.getString("password");
                        n.putArgument("password", pwd);
                    }
                    break;
                    case USER: {
                        String group = obj.getString("group");
                        n.putArgument("group", group);
                    }
                }

                nodeList.add(n);
                log.info("Create node {}", n.toString());
            }
        } catch (IOException e) {
            throw new ConfigInitializationException(e);
        }
    }


    private void makeTemplateConfigFile(Path path) throws Exception {
        JSONArray arr = new JSONArray();
        JSONObject obj = new JSONObject();
        obj.put("name", "default");
        obj.put("port", 2020);
        obj.put("max-client", 10);
        obj.put("cert-port", 7060);
        obj.put("encrypt", "OpenSSL");
        obj.put("auth-type", "simple");
        obj.put("password", UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        arr.add(obj);

        try(FileWriter writer = new FileWriter(path.toFile())) {
            writer.write(arr.toJSONString());
        }
    }

    public Path getLocation() {
        return location;
    }

    public Node[] getServerNode() {
        return nodeList.toArray(new Node[0]);
    }

    /**
     * 服务器配置节点类
     * 每个节点需要绑定不同的端口，并拥有各自的配置方案
     */
    public static class Node implements Named {
        public final String name;   //节点名称
        public final int port;      //绑定端口
        public final int certPort;  //收发CA证书端口
        public final int maxClient; //最大客户端连接数
        public final ClientAuthType authType; //认证方式
        public final ClientEncryptType encryptType;   //加密方式

        //认证参数
        private final Map<String, String> args = new HashMap<>(4);

        private Node(String name, int port, int certPort, int maxClient, ClientAuthType authType, ClientEncryptType encryptType) {
            this.name = Objects.requireNonNull(name);
            this.port = port;
            this.certPort = certPort;
            this.maxClient = maxClient;
            this.authType = Objects.requireNonNull(authType);
            this.encryptType = Objects.requireNonNull(encryptType);
        }

        @Override
        public String getName() {
            return name;
        }

        private void putArgument(String key, String value) {
            args.put(key, value);
        }

        public String getArgument(String key) {
            synchronized (args) {
                return args.get(key);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append("name:").append(name).append(" port:")
                    .append(port).append(" cert-port:").append(certPort)
                    .append(" maxClient:").append(maxClient).append(" Auth:")
                    .append(authType.name());

            switch (authType) {
                case SIMPLE:
                    sb.append(" password:").append(args.get("password"));
                    break;
                case USER:
                    sb.append(" group:").append(args.get("group"));
                    break;
            }

            sb.append(" Encrypt:").append(encryptType.name());

            return sb.toString();
        }
    }
}
