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
package com.lzf.flyingsocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultConfigManager<T extends Component<?> & Environment>
        extends AbstractModule<T> implements ConfigManager<T> {

    /**
     * Slf4j日志
     */
    private final Logger log;

    /**
     * 配置存储Map
     */
    private final Map<String, Config> configMap = new ConcurrentHashMap<>();

    /**
     * 配置事件监听器集合
     */
    private final List<ConfigEventListener> listeners = new CopyOnWriteArrayList<>();

    public DefaultConfigManager(T belongComponent, String name) {
        super(belongComponent, name);
        log = LoggerFactory.getLogger(name);
    }

    public DefaultConfigManager(T belongComponent) {
        this(belongComponent, "ConfigManager");
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
            throw e;
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
    public void removeConfig(String name) {
        Config cfg;
        synchronized (configMap) {
            cfg = configMap.remove(name);
        }
        if (cfg != null)
            fireConfigEvent(Config.REMOVE_EVENT, cfg);
    }

    @Override
    public boolean saveConfig(Config config) {
        if (config.canSave()) {
            try {
                config.save();
                return true;
            } catch (Exception e) {
                log.warn("Save config '{}' occur a exception", config.getName(), e);
                return false;
            }
        }

        return false;
    }

    @Override
    public void saveAllConfig() {
        synchronized (configMap) {
            for (Config c : configMap.values()) {
                try {
                    if (c.canSave())
                        c.save();
                } catch (Exception e) {
                    log.warn("Save config '{}' occur a exception", c.getName(), e);
                }
            }
        }
    }

    @Override
    public Config getConfig(String name) {
        return configMap.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <C extends Config> C getConfig(String name, Class<C> requireType) {
        return (C) configMap.get(name);
    }

    @Override
    public void registerConfigEventListener(ConfigEventListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    @Override
    public void removeConfigEventListener(ConfigEventListener listener) {
        listeners.remove(Objects.requireNonNull(listener));
    }


    protected void fireConfigEvent(ConfigEvent configEvent) {
        for (ConfigEventListener listener : listeners)
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
        log.info("System Property set: {}={}", key, value);
        getComponent().setSystemProperties(key, value);
    }

    @Override
    public InputStream loadResource(String path) throws IOException {
        return getComponent().loadResource(path);
    }
}
