package com.lzf.flyingsocks;

import java.util.EventObject;

public class ConfigEvent extends EventObject {

    private final String event;

    private final ConfigManager configManager;

    public ConfigEvent(String event, Config source, ConfigManager configManager) {
        super(source);
        this.event = event;
        this.configManager = configManager;
    }

    public final String getEvent() {
        return event;
    }

    public final ConfigManager getConfigManager() {
        return configManager;
    }
}
