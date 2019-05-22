package com.lzf.flyingsocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表示顶层组件，顶层组件不可拥有父组件
 * 顶层组件除了具有AbstractComponent所有特性外，还可以保存环境参数，加载环境资源，并具有配置管理器
 * @see com.lzf.flyingsocks.AbstractComponent
 * @see com.lzf.flyingsocks.Environment
 */
public abstract class TopLevelComponent extends AbstractComponent<VoidComponent> implements Environment {

    /**
     * 保存系统环境变量，通过System.getenv()获取
     */
    private final Map<String, String> environmentVariableMap = new ConcurrentHashMap<>();

    /**
     * 保存系统变量
     */
    private final Map<String, String> systemProperties = new ConcurrentHashMap<>();

    /**
     * 配置管理器实例
     */
    private ConfigManager<TopLevelComponent> configManager = new DefaultConfigManager<>(this);

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
    protected void initInternal() {
        try {
            //加载这个类以便支持‘classpath:’类型的URL
            Class.forName("com.lzf.flyingsocks.url.ClasspathURLHandlerFactory");
        } catch (ClassNotFoundException e) {
            throw new ComponentException(e);
        }

        environmentVariableMap.putAll(System.getenv());

        for(Map.Entry<Object, Object> entry : System.getProperties().entrySet())
            systemProperties.put((String)entry.getKey(), (String)entry.getValue());

        super.initInternal();
    }


    @Override
    public String getEnvironmentVariable(String key) {
        return environmentVariableMap.get(key);
    }

    @Override
    public String getSystemProperties(String key) {
        return systemProperties.get(key);
    }

    @Override
    public void setSystemProperties(String key, String value) {
        systemProperties.put(key, value);
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
