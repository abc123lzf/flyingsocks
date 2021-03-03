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
package com.lzf.flyingsocks.misc;

import com.lzf.flyingsocks.Lifecycle;
import com.lzf.flyingsocks.LifecycleEvent;
import com.lzf.flyingsocks.LifecycleEventListener;
import com.lzf.flyingsocks.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.lzf.flyingsocks.Lifecycle.BEFORE_INIT_EVENT;
import static com.lzf.flyingsocks.Lifecycle.AFTER_INIT_EVENT;
import static com.lzf.flyingsocks.Lifecycle.BEFORE_START_EVENT;
import static com.lzf.flyingsocks.Lifecycle.AFTER_START_EVENT;
import static com.lzf.flyingsocks.Lifecycle.BEFORE_STOP_EVENT;
import static com.lzf.flyingsocks.Lifecycle.AFTER_STOP_EVENT;
import static com.lzf.flyingsocks.Lifecycle.BEFORE_RESTART_EVENT;
import static com.lzf.flyingsocks.Lifecycle.AFTER_RESTART_EVENT;

/**
 * 对Lifecycle对象的生命周期变动进行日志输出
 * 若日志级别为INFO以下，则INSTANCE为null
 */
public class LifecycleLoggerListener implements LifecycleEventListener {

    public static final LifecycleLoggerListener INSTANCE;

    private static final Logger log;

    static {
        log = LoggerFactory.getLogger("LifecycleLogger");
        if (log.isInfoEnabled()) {
            INSTANCE = new LifecycleLoggerListener();
        } else {
            INSTANCE = null;
        }
    }

    private LifecycleLoggerListener() {
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        Object src;
        if (!((src = event.getSource()) instanceof Lifecycle))
            return;

        String name;
        if (src instanceof Named) {
            name = ((Named) src).getName();
        } else {
            name = src.getClass().getSimpleName();
        }

        switch (event.getType()) {
            case BEFORE_INIT_EVENT:
                log.info("{} ready to initial.", name);
                break;
            case AFTER_INIT_EVENT:
                log.info("{} initial complete.", name);
                break;
            case BEFORE_START_EVENT:
                log.info("{} ready to start.", name);
                break;
            case AFTER_START_EVENT:
                log.info("{} start complete.", name);
                break;
            case BEFORE_STOP_EVENT:
                log.info("{} ready to stop.", name);
                break;
            case AFTER_STOP_EVENT:
                log.info("{} stop complete", name);
                break;
            case BEFORE_RESTART_EVENT:
                log.info("{} ready to restart.", name);
                break;
            case AFTER_RESTART_EVENT:
                log.info("{} restart complete.", name);
                break;
        }
    }
}
