package com.lzf.flyingsocks.server;

import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.TopLevelComponent;
import com.lzf.flyingsocks.server.core.ProxyProcessor;


public class StandardServer extends TopLevelComponent implements Server {

    static {
        try {
            Class.forName("com.lzf.flyingsocks.url.ClasspathURLHandlerFactory");
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    private ServerConfig serverConfig;

    public StandardServer() {
        super("Server");
    }

    @Override
    protected void initInternal() {
        serverConfig = new ServerConfig(getConfigManager());
        getConfigManager().registerConfig(serverConfig);

        ServerConfig.Node[] nodes = serverConfig.getServerNode();
        for(ServerConfig.Node node : nodes) {
            addComponent(new ProxyProcessor(this, node));
        }
        super.initInternal();
    }

    @Override
    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    @Override
    public ConfigManager<?> getConfigManager() {
        return super.getConfigManager();
    }
}
