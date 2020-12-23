package com.lzf.flyingsocks;

import java.io.Serializable;

/**
 * 表示一个配置对象，需要和ConfigManager搭配使用
 *
 * @see com.lzf.flyingsocks.ConfigManager
 */
public interface Config extends Named, Serializable {

    /**
     * 配置注册事件
     */
    String REGISTER_EVENT = "config_register_event";

    /**
     * 配置更新事件
     */
    String UPDATE_EVENT = "config_update_event";

    /**
     * 配置删除事件
     */
    String REMOVE_EVENT = "config_remove_event";

    /**
     * 获取该配置所属的配置管理器
     *
     * @return 配置管理器
     */
    ConfigManager<?> configManager();

    /**
     * 初始化配置
     */
    void initialize() throws ConfigInitializationException;

    /**
     * @return 该配置是否支持保存
     */
    default boolean canSave() {
        return false;
    }

    /**
     * 保存配置
     */
    default void save() throws Exception {
        throw new UnsupportedOperationException("Config " + getName() + " at ConfigManager " +
                configManager().getName() + " NOT support save.");
    }

}
