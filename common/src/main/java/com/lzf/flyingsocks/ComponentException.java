package com.lzf.flyingsocks;

/**
 * 用于描述组件中发生的异常
 */
public class ComponentException extends RuntimeException {

    private static final long serialVersionUID = 4087418469075268501L;

    public ComponentException() {
        super();
    }

    public ComponentException(String message) {
        super(message);
    }

    public ComponentException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComponentException(Throwable cause) {
        super(cause);
    }
}
