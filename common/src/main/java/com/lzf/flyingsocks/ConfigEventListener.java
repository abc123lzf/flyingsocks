package com.lzf.flyingsocks;

import java.util.EventListener;

@FunctionalInterface
public interface ConfigEventListener extends EventListener {

    void configEvent(ConfigEvent configEvent);

}
