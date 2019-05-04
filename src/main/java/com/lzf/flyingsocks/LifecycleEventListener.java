package com.lzf.flyingsocks;

import java.util.EventListener;

@FunctionalInterface
public interface LifecycleEventListener extends EventListener {

    void lifecycleEvent(LifecycleEvent event);

}
