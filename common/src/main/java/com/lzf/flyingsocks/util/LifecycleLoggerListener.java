package com.lzf.flyingsocks.util;

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
