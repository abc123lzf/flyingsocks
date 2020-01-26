package com.lzf.flyingsocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 表示顶层组件，顶层组件不可拥有父组件
 * 顶层组件除了具有AbstractComponent所有特性外，还可以保存环境参数，加载环境资源，并具有配置管理器
 * @see com.lzf.flyingsocks.AbstractComponent
 * @see com.lzf.flyingsocks.Environment
 */
public abstract class TopLevelComponent extends AbstractComponent<VoidComponent> implements Environment {

    static {
        try {
            //加载这个类以便支持‘classpath:’类型的URL
            Class.forName("com.lzf.flyingsocks.url.ClasspathURLHandlerFactory");
        } catch (ClassNotFoundException e) {
            throw new ComponentException(e);
        }
    }

    /**
     * 配置管理器实例
     */
    private final ConfigManager<TopLevelComponent> configManager = new DefaultConfigManager<>(this);

    protected TopLevelComponent() {
        super();
        addModule(configManager);
    }

    protected TopLevelComponent(String name) {
        super(name, null);
        addModule(configManager);
    }

    @Override
    public final VoidComponent getParentComponent() {
        return null;
    }


    @Override
    public String getEnvironmentVariable(String key) {
        return System.getenv(key);
    }

    @Override
    public String getSystemProperties(String key) {
        return System.getProperty(key);
    }

    @Override
    public void setSystemProperties(String key, String value) {
        System.setProperty(key, value);
    }

    @Override
    public InputStream loadResource(String path) throws IOException {
        try {
            URL url = new URL(path);
            return url.openStream();
        } catch (MalformedURLException e) {
            File file = new File(path);
            if(file.exists() && file.isFile()) {
                return new FileInputStream(file);
            } else {
                if (log.isWarnEnabled())
                    log.warn("URL or file path error", e);
                return null;
            }
        } catch (IOException e) {
            if(log.isErrorEnabled())
                log.error("load resource {} occur a IOException", path);
            throw e;
        }
    }

    protected ConfigManager<?> getConfigManager() {
        return configManager;
    }
}
