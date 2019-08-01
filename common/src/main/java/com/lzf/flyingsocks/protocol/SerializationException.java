package com.lzf.flyingsocks.protocol;

public class SerializationException extends Exception {

    /**
     * 发生序列化错误的对象，可能为Message类型（序列化）
     * 可能为ByteBuf反序列化
     */
    private Object errorMessage;

    public SerializationException(String msg) {
        super(msg);
    }

    public SerializationException(String msg, Throwable t) {
        super(msg, t);
    }

    public SerializationException(Throwable t) {
        super(t);
    }

    SerializationException(String msg, Object message) {
        super(msg);
        this.errorMessage = message;

    }

    public SerializationException(String msg, Throwable t, Object message) {
        super(msg, t);
        this.errorMessage = message;
    }

    public void setErrorMessage(Object errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Object getErrorMessage() {
        return errorMessage;
    }
}
