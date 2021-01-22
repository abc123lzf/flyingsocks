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
package com.lzf.flyingsocks.url;


import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * 用于处理classpath开头的URL，这个URL会从加载这个类的类加载器的目录下搜索资源并返回其InputStream
 * URL格式为: classpath://folder/file/...
 */
public final class ClasspathURLHandlerFactory implements URLStreamHandlerFactory {

    private static final String PROTOCOL = "classpath";

    static {
        ClassLoader cl = ClasspathURLHandlerFactory.class.getClassLoader();
        URL.setURLStreamHandlerFactory(new ClasspathURLHandlerFactory(cl));
    }

    private final ClassLoader classLoader;

    private ClasspathURLHandlerFactory(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (protocol.toLowerCase().equals(PROTOCOL)) {
            return new ClasspathURLHandler(classLoader);
        } else {
            return null;
        }
    }
}
