package com.lzf.flyingsocks.client.gui.swt;

import java.util.prefs.Preferences;

public class Demo2 {

    public static void main(String[] args) throws Exception {
        Preferences pre = Preferences.userRoot();
        System.out.println(pre);
        Preferences node = pre.node("/SOFTWARE/Microsoft/Windows/CurrentVersion/Internet Settings");
        System.out.println(node.getInt("MigrateProxy", -1));
    }
}
