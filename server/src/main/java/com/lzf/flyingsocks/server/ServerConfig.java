package com.lzf.flyingsocks.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.protocol.AuthMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        try {
            Class.forName("com.lzf.flyingsocks.url.ClasspathURLHandlerFactory");
        } catch (ClassNotFoundException e) {
            throw new ComponentException(e);
        }

        InputStream is;
        try {
            is = configManager.loadResource("classpath://server.json");
        } catch (IOException e) {
            if(log.isErrorEnabled())
                log.error("server.json doesn't exists.", e);
            throw new ConfigInitializationException(e);
        }

        byte[] b = new byte[10240 * 5];

        int size;
        try {
            size = is.read(b);
        } catch (IOException e) {
            if(log.isErrorEnabled())
                log.error("load server.json occur a exception.", e);
            throw new ConfigInitializationException(e);
        }

        String json = new String(b, 0, size, Charset.forName("UTF-8"));

        JSONArray arr;
        try {
            arr = JSON.parseArray(json);
        } catch (JSONException e) {
            if(log.isErrorEnabled())
                log.error("server.json format error.", e);
            throw new ConfigInitializationException(e);
        }

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
