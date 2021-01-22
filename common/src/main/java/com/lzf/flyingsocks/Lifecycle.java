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
