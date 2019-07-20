package com.lzf.flyingsocks;

public class LifecycleException extends RuntimeException {

    private static final long serialVersionUID = 4087418469075268500L;

    public LifecycleException() {
        super();
    }

    public LifecycleException(String message) {
        super(message);
    }

    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
    }

    public LifecycleException(Throwable cause) {
        super(cause);
    }
}
