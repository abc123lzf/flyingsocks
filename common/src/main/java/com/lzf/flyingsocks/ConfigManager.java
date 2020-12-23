package com.lzf.flyingsocks;

public interface ConfigManager<T extends Component<?>> extends Module<T>, Environment {

    /**
     * 注册配置
     *
     * @param config 配置对象
     */
    void registerConfig(Config config) throws ConfigInitializationException;

    /**
     * 更新配置
     *
     * @param config 配置对象
     */
    void updateConfig(Config config);

    /**
     * 删除配置
     *
     * @param config 配置对象
     */
    void removeConfig(Config config);

    /**
     * 删除配置，通过名称
     *
     * @param name 配置名称
     */
    void removeConfig(String name);

    /**
     * 保存配置
     *
     * @param config 配置对象
     * @return 是否成功
     */
    boolean saveConfig(Config config);

    /**
     * 保存该配置管理器中所有的配置
     */
    void saveAllConfig();


    /**
     * 获取配置
     *
     * @param name 配置名称
     * @return 配置
     */
    Config getConfig(String name);

    /**
     * 获取配置并进行类型转换
     *
     * @param name        配置名称
     * @param requireType 配置Class对象
     * @param <C>         配置类型
     * @return 配置
     */
    <C extends Config> C getConfig(String name, Class<C> requireType);

    /**
     * 注册配置事件监听器
     *
     * @param listener 监听器对象
     */
    void registerConfigEventListener(ConfigEventListener listener);

    /**
     * 删除配置事件监听器
     *
     * @param listener 监听器对象
     */
    void removeConfigEventListener(ConfigEventListener listener);

}
