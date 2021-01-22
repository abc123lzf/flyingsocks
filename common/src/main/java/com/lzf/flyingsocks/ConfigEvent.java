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

    private final ConfigManager<?> configManager;

    public ConfigEvent(String event, Config source, ConfigManager<?> configManager) {
        super(source);
        this.event = event;
        this.configManager = configManager;
    }

    public final String getEvent() {
        return event;
    }

    public final ConfigManager<?> getConfigManager() {
        return configManager;
    }
}
