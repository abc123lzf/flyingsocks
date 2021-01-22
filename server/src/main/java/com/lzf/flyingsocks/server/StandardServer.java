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
