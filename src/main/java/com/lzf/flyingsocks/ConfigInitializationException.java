package com.lzf.flyingsocks;

public class ConfigInitializationException extends Exception {

    public ConfigInitializationException() { }

    public ConfigInitializationException(String msg) {
        super(msg);
    }

    public ConfigInitializationException(String msg, Throwable t) {
        super(msg, t);
    }

    public ConfigInitializationException(Throwable t) {
        super(t);
    }
}
