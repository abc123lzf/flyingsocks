package com.lzf.flyingsocks;

import com.lzf.flyingsocks.url.ClasspathURLHandlerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表示顶层组件，顶层组件不可拥有父组件
 */
public abstract class TopLevelComponent extends AbstractComponent<VoidComponent> implements Environment {

    private final Map<String, String> environmentVariableMap = new ConcurrentHashMap<>();

    private final Map<String, String> systemProperties = new ConcurrentHashMap<>();

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
                if(log.isErrorEnabled())
                    log.error("URL error", e);
                return null;
            } catch (IOException e) {
                if(log.isErrorEnabled())
                    log.error("can not found resource {}.", path);
                throw e;
            }



    }

    public final ConfigManager<?> getConfigManager() {
        return configManager;
    }
}
