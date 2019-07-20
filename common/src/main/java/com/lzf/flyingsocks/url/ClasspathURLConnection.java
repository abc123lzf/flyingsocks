package com.lzf.flyingsocks.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

class ClasspathURLConnection extends URLConnection {

    private final ClassLoader classLoader;
    private final String location;

    ClasspathURLConnection(URL url, ClassLoader classLoader) {
        super(url);
        this.classLoader = Objects.requireNonNull(classLoader);
        location = url.getAuthority() + url.getPath();
    }

    @Override
    public void connect() throws IOException {
        if(classLoader.getResource(location) == null)
            throw new IOException("Classpath resource [" + location + "] not found");
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream is = classLoader.getResourceAsStream(location);
        if(is == null)
            throw new IOException("Classpath resource [" + location + "] not found");
        return is;
    }
}