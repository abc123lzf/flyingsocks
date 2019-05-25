package com.lzf.flyingsocks.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.protocol.AuthMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

public class ServerConfig extends AbstractConfig implements Config {
    private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);

    private final List<Node> nodeList = new ArrayList<>();

    ServerConfig(ConfigManager<?> configManager) {
        super(configManager, "Config-Server");
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        try(InputStream is = configManager.loadResource("classpath://config.properties")) {
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
                byte[] b = new byte[102400];
                int len = cis.read(b);
                String json = new String(b, 0, len, Charset.forName("UTF-8"));
                JSONArray arr = JSON.parseArray(json);
                for(int i = 0; i < arr.size(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String name = obj.getString("name");
                    int port = obj.getIntValue("port");
                    int client = obj.getIntValue("max-client");

                    EncrtptType encrtptType = EncrtptType.valueOf(obj.getString("encrypt"));
                    AuthType authType = AuthType.valueOf(obj.getString("auth-type").toUpperCase());

                    Node n = new Node(name, port, client, authType, encrtptType);

                    switch (authType) {
                        case SIMPLE: {
                            String pwd = obj.getString("password");
                            n.putArgument("password", pwd);
                        } break;
                        case USER: {
                            String group = obj.getString("group");
                            n.putArgument("group", group);
                        }
                    }

                    nodeList.add(n);
                }
            }

        } catch (Exception e) {
            throw new ConfigInitializationException(e);
        }
    }


    private void makeTemplateConfigFile(File file) throws Exception {
        JSONArray arr = new JSONArray();
        JSONObject obj = new JSONObject();
        obj.put("name", "default");
        obj.put("port", 2020);
        obj.put("max-client", 10);
        obj.put("encrypt", "OpenSSL");
        obj.put("auth-type", "simple");
        obj.put("password", UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        arr.add(obj);

        FileWriter writer = new FileWriter(file);
        writer.write(arr.toJSONString());
        writer.close();
    }


    public Node[] getServerNode() {
        return nodeList.toArray(new Node[nodeList.size()]);
    }

    /**
     * 服务器指定的认证类型
     */
    public enum AuthType {
        SIMPLE(AuthMessage.AuthMethod.SIMPLE), USER(AuthMessage.AuthMethod.USER);

        private final AuthMessage.AuthMethod authMethod;

        AuthType(AuthMessage.AuthMethod authMethod) {
            this.authMethod = authMethod;
        }

        /**
         * 对客户端认证报文进行比对
         * @param node 服务器配置
         * @param authMessage 客户端认证报文
         * @return 是否通过认证
         */
        public boolean doAuth(Node node, AuthMessage authMessage) {
            //如果认证方式不匹配
            if(this.authMethod != authMessage.getAuthMethod()) {
                return false;
            }

            List<String> keys = authMessage.getAuthMethod().getContainsKey();
            //比对认证信息
            for(String key : keys) {
                if(!node.getArgument(key).equals(authMessage.getContent(key)))
                    return false;
            }

            return true;
        }
    }

    public enum EncrtptType {
        None, OpenSSL, JKS;
    }

    /**
     * 服务器配置节点类
     * 每个节点需要绑定不同的端口，并拥有各自的配置方案
     */
    public static class Node {
        public final String name;   //节点名称
        public final int port;      //绑定端口
        public final int maxClient; //最大客户端连接数
        public final AuthType authType; //认证方式
        public final EncrtptType encrtptType;   //加密方式

        //认证参数
        private final Map<String, String> args = new HashMap<>(4);

        private Node(String name, int port, int maxClient, AuthType authType, EncrtptType encrtptType) {
            this.name = Objects.requireNonNull(name);
            this.port = port;
            this.maxClient = maxClient;
            this.authType = Objects.requireNonNull(authType);
            this.encrtptType = Objects.requireNonNull(encrtptType);
        }

        private void putArgument(String key, String value) {
            args.put(key, value);
        }

        public String getArgument(String key) {
            synchronized (args) {
                return args.get(key);
            }
        }
    }
}
