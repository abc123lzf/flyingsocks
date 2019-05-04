package com.lzf.flyingsocks;

import java.util.Objects;

public abstract class AbstractConfig implements Config {

    private final String name;

    protected final ConfigManager<?> configManager;

    private boolean initial = false;

    protected AbstractConfig(ConfigManager<?> configManager, String name) {
        this.configManager = Objects.requireNonNull(configManager);
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public final ConfigManager<?> configManager() {
        return configManager;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public synchronized final void initialize() throws ConfigInitializationException {
        if(initial) {
            throw new IllegalStateException("can not initial double time in this config " + getName());
        }

        try {
            initInternal();
            initial = true;
        } catch (ConfigInitializationException e) {
            throw e;
        }
    }

    protected abstract void initInternal() throws ConfigInitializationException;

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(this == obj)
            return true;

        if(obj.getClass() == getClass()) {
            AbstractConfig c = (AbstractConfig) obj;
            if(c.configManager.equals(this.configManager) && c.name.equals(this.name))
                return true;
        }

        return false;
    }
}
