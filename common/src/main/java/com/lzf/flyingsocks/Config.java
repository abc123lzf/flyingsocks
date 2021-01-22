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
