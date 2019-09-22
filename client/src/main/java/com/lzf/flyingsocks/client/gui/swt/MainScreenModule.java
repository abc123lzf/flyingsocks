package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;

import java.util.Objects;

class MainScreenModule extends AbstractModule<SWTViewComponent> {

    public MainScreenModule(SWTViewComponent component) {
        super(Objects.requireNonNull(component), "Main-Screen");
        initial();
    }

    private void initial() {

    }
}
