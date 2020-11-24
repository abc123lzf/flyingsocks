package com.lzf.flyingsocks.server;

import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.TopLevelComponent;
import com.lzf.flyingsocks.server.core.ProxyProcessor;
import com.lzf.flyingsocks.server.db.TextUserDatabase;
import com.lzf.flyingsocks.server.db.UserDatabase;


public class StandardServer extends TopLevelComponent implements Server {

    static {
        try {
            Class.forName("com.lzf.flyingsocks.url.ClasspathURLHandlerFactory");
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    private ServerConfig serverConfig;

    private UserDatabase userDatabase;

    public StandardServer() {
        super("Server");
    }

    @Override
    protected void initInternal() {
        serverConfig = new ServerConfig(getConfigManager());
        getConfigManager().registerConfig(serverConfig);

        ServerConfig.Node[] nodes = serverConfig.getServerNode();
        for (ServerConfig.Node node : nodes) {
            addComponent(new ProxyProcessor(this, node));
        }

        TextUserDatabase db = new TextUserDatabase(getConfigManager());
        getConfigManager().registerConfig(db);

        this.userDatabase = db;
        super.initInternal();
    }

    @Override
    public ServerConfig config() {
        return serverConfig;
    }

    @Override
    public ConfigManager<?> getConfigManager() {
        return super.getConfigManager();
    }

    @Override
    public UserDatabase getUserDatabase() {
        return userDatabase;
    }

    @Override
    public String getSystemProperties(String key) {
        return super.getSystemProperties(key);
    }

    @Override
    public String getEnvironmentVariable(String key) {
        return super.getEnvironmentVariable(key);
    }

    @Override
    public void setSystemProperties(String key, String value) {
        super.setSystemProperties(key, value);
    }
}
