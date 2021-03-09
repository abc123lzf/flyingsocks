/*
 * Copyright (c) 2019 abc123lzf <abc123lzf@126.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lzf.flyingsocks.protocol;

/**
 * 序列化时数据格式有误抛出的异常
 */
public class SerializationException extends Exception {

    private static final StackTraceElement[] NONE_STACK = new StackTraceElement[0];

    private final Class<? extends Message> messageClass;

    public SerializationException(String msg) {
        super(msg);
        this.messageClass = null;
    }

    public SerializationException(String msg, Throwable t) {
        super(msg, t);
        this.messageClass = null;
    }

    public SerializationException(Throwable t) {
        super(t);
        this.messageClass = null;
    }

    public SerializationException(Class<? extends Message> messageClass, String msg) {
        super(toMessage(messageClass, msg));
        this.messageClass = messageClass;
    }

    public SerializationException(Class<? extends Message> messageClass, Throwable t) {
        super("[" + messageClass.getName() + "]", t);
        this.messageClass = messageClass;
    }

    public SerializationException(Class<? extends Message> messageClass, String message, Throwable t) {
        super("[" + messageClass.getName() + "] " + message, t);
        this.messageClass = messageClass;
    }


    private static String toMessage(Class<? extends Message> messageClass, String msg) {
        return "[" + messageClass.getName() + "]" + msg;
    }

    public Class<? extends Message> getMessageClass() {
        return messageClass;
    }
    /**
     * 不爬栈
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        setStackTrace(NONE_STACK);
        return this;
    }
}
