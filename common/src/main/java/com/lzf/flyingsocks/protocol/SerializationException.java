package com.lzf.flyingsocks.protocol;

public class SerializationException extends Exception {

    public SerializationException(String msg) {
        super(msg);
    }

    public SerializationException(String msg, Throwable t) {
        super(msg, t);
    }

    public SerializationException(Throwable t) {
        super(t);
    }
}
