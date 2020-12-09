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
