package com.lzf.flyingsocks;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultConfigManager<T extends Component<?> & Environment> extends AbstractModule<T> implements ConfigManager<T> {

    private final Map<String, Config> configMap = new ConcurrentHashMap<>();

    private final List<ConfigEventListener> listeners = new CopyOnWriteArrayList<>();

    public DefaultConfigManager(T belongComponent, String name) {
        super(belongComponent, name);
    }

    public DefaultConfigManager(T belongComponent) {
        super(belongComponent, "ConfigManager");
    }

    @Override
    public void registerConfig(Config config) {
        String name;
        synchronized (configMap) {
            if (configMap.containsKey(name = config.getName()))
                throw new IllegalStateException(String.format("config %s is already exists", name));
            configMap.put(name, config);
        }

        try {
            config.initialize();
        } catch (ConfigInitializationException e) {
            throw new ComponentException(e);
        } catch (Exception e) {
            synchronized (configMap) {
                configMap.remove(name);
                throw new ComponentException(String.format("register config %s occur a exception", name), new ConfigInitializationException(e));
            }
        }

        fireConfigEvent(Config.REGISTER_EVENT, config);
    }

    @Override
    public void updateConfig(Config config) {
        String name;
        synchronized (configMap) {
            if (!configMap.containsKey(name = config.getName()))
                throw new IllegalStateException(String.format("config %s is not exists", name));

            configMap.put(name, config);
        }

        fireConfigEvent(Config.UPDATE_EVENT, config);
    }

    @Override
    public void removeConfig(Config config) {
        String name;
        synchronized (configMap) {
            if (!configMap.containsKey(name = config.getName()))
                throw new IllegalStateException(String.format("config %s is not exists", name));
            configMap.remove(name);
        }
        fireConfigEvent(Config.REMOVE_EVENT, config);
    }

    @Override
    public Config getConfig(String name) {
        return configMap.get(name);
    }

    @Override @SuppressWarnings("unchecked")
    public final  <T extends Config> T getConfig(String name, Class<T> requireType) {
        return (T) configMap.get(name);
    }

    @Override
    public void registerConfigEventListener(ConfigEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConfigEventListener(ConfigEventListener listener) {
        listeners.remove(listener);
    }


    protected void fireConfigEvent(ConfigEvent configEvent) {
        for(ConfigEventListener listener : listeners)
            listener.configEvent(configEvent);
    }

    private void fireConfigEvent(String type, Config config) {
        ConfigEvent event = new ConfigEvent(type, config, this);
        fireConfigEvent(event);
    }

    @Override
    public String getEnvironmentVariable(String key) {
        return getComponent().getEnvironmentVariable(key);
    }

    @Override
    public String getSystemProperties(String key) {
        return getComponent().getSystemProperties(key);
    }

    @Override
    public void setSystemProperties(String key, String value) {
        getComponent().setSystemProperties(key, value);
    }

    @Override
    public InputStream loadResource(String path) throws IOException {
        return getComponent().loadResource(path);
    }
}
