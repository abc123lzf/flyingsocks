package com.lzf.flyingsocks;

public interface Lifecycle {

    String BEFORE_INIT_EVENT = "before_init";
    String AFTER_INIT_EVENT = "after_init";

    String BEFORE_START_EVENT = "before_start";
    String AFTER_START_EVENT = "after_start";

    String BEFORE_STOP_EVENT = "before_stop";
    String AFTER_STOP_EVENT = "after_stop";

    String BEFORE_RESTART_EVENT = "before_restart";
    String AFTER_RESTART_EVENT = "after_restart";

    /**
     * 初始化组件
     */
    void init() throws LifecycleException;

    /**
     * 启动组件
     */
    void start() throws LifecycleException;

    /**
     * 停止当前组件
     */
    void stop() throws LifecycleException;

    /**
     * 重新启动当前组件
     */
    void restart() throws LifecycleException;

    /**
     * 当前组件所处的状态
     *
     * @return LifecycleState枚举对象
     */
    LifecycleState getState();

    /**
     * 添加事件监听器
     */
    void addLifecycleEventListener(LifecycleEventListener listener);

    /**
     * 移除事件监听器
     *
     * @param listener 监听器对象
     */
    void removeLifecycleEventListener(LifecycleEventListener listener);

}
