package com.lzf.flyingsocks.client.gui.swing.model;

import com.lzf.flyingsocks.Named;

/**
 * @author lizifan 695199262@qq.com
 * @since 2021.9.25 21:08
 */
public class ServerGroupVO implements Named {

    private String name;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public String toString() {
        return getName();
    }
}
