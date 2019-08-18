package com.lzf.flyingsocks;

import java.util.EventListener;

/**
 * 配置中心事件监听器
 * 可以在配置被添加、修改、删除时得到通知
 */
@FunctionalInterface
public interface ConfigEventListener extends EventListener {

    void configEvent(ConfigEvent configEvent);

}
