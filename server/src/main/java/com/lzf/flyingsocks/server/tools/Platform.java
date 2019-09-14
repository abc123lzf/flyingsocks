package com.lzf.flyingsocks.server.tools;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

abstract class Platform {

    private static volatile Boolean windows;
    private static volatile Boolean macOSX;
    private static volatile Boolean android;

    static boolean isWindows() {
        if(windows == null) {
            return windows = getProperty("os.name", "").toLowerCase(Locale.US).contains("win");
        }

        return windows;
    }

    static boolean isMacOSX() {
        if(macOSX == null) {
            String name = getProperty("os.name", "").toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
            return macOSX = name.startsWith("macosx") || name.startsWith("osx");
        }

        return macOSX;
    }

    static boolean isAndroid() {
        if(android == null) {
            return android = "Dalvik".equals(getProperty("java.vm.name", null));
        }

        return android;
    }

    static boolean isLinux() {
        return !isWindows() && !isMacOSX() && !isAndroid();
    }

    private static String getProperty(final String key, String def) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty.");
        }

        String value = null;
        try {
            if (System.getSecurityManager() == null) {
                value = System.getProperty(key);
            } else {
                PrivilegedAction<String> action = () -> System.getProperty(key);
                value = AccessController.doPrivileged(action);
            }
        } catch (SecurityException e) {
            System.err.println(String.format("Unable to retrieve a system property '%s'; default values will be used.", key));
            e.printStackTrace(System.err);
        }

        if (value == null) {
            return def;
        }

        return value;
    }



    private Platform() {
        throw new UnsupportedOperationException();
    }

}
