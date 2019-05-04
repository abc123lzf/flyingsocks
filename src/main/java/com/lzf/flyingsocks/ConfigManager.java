package com.lzf.flyingsocks;

public interface ConfigManager<T extends Component<?>> extends Module<T>, Environment {

    /**
     * 注册配置
     * @param config 配置对象
     */
    void registerConfig(Config config);

    /**
     * 更新配置
     * @param config 配置对象
     */
    void updateConfig(Config config);

    /**
     *
     * @param config
     */
    void removeConfig(Config config);

    /**
     * 获取配置
     * @param name 配置名称
     * @return 配置
     */
    Config getConfig(String name);

    /**
     * 获取配置并进行类型转换
     * @param name 配置名称
     * @param requireType 配置Class对象
     * @param <C> 配置类型
     * @return 配置
     */
    <C extends Config> C getConfig(String name, Class<C> requireType);

    /**
     * 注册配置事件监听器
     * @param listener 监听器对象
     */
    void registerConfigEventListener(ConfigEventListener listener);

    /**
     * 删除配置事件监听器
     * @param listener
     */
    void removeConfigEventListener(ConfigEventListener listener);

}
