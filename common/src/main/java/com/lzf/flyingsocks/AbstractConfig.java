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

import java.util.Objects;

/**
 * 配置模板类
 *
 * @see com.lzf.flyingsocks.Config
 */
public abstract class AbstractConfig implements Config {

    /**
     * 配置名称
     */
    private final String name;

    /**
     * 隶属的配置管理器
     */
    protected final ConfigManager<?> configManager;

    /**
     * 是否已经初始化过
     */
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
        if (initial) {
            throw new IllegalStateException("can not initial double time in this config " + getName());
        }

        try {
            initInternal();
            initial = true;
        } catch (ConfigInitializationException e) {
            throw e;
        }
    }

    protected void initInternal() throws ConfigInitializationException { }

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
        if (obj == null)
            return false;
        if (this == obj)
            return true;

        if (obj.getClass() == getClass()) {
            AbstractConfig c = (AbstractConfig) obj;
            return c.configManager.equals(this.configManager) && c.name.equals(this.name);
        }

        return false;
    }
}
