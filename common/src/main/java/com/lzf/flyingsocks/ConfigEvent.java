package com.lzf.flyingsocks;

import java.util.EventObject;

/**
 * 配置管理器中发生的事件，例如配置更新事件、配置删除事件、配置注册事件
 *
 * @see com.lzf.flyingsocks.ConfigManager
 * @see com.lzf.flyingsocks.Config
 * @see com.lzf.flyingsocks.ConfigEventListener
 */
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
