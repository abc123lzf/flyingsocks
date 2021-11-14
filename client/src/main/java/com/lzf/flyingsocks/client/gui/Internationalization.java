package com.lzf.flyingsocks.client.gui;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author lizifan 695199262@qq.com
 * @since 2021.8.21 23:48
 */
public final class Internationalization {

    private static final ResourceBundle RESOURCE_BUNDLE;

    static {
        Locale locale = Locale.getDefault();
        RESOURCE_BUNDLE = ResourceBundle.getBundle("META-INF/i18n/swtui", locale);
    }

    public static ResourceBundle getResourceBundle() {
        return RESOURCE_BUNDLE;
    }

    public static String get(String key) {
        if (key == null) {
            return "null";
        }
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
