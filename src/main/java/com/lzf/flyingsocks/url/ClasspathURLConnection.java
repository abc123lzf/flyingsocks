package com.lzf.flyingsocks.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

class ClasspathURLConnection extends URLConnection {

    private final ClassLoader classLoader;

    ClasspathURLConnection(URL url, ClassLoader classLoader) {
        super(url);
        this.classLoader = Objects.requireNonNull(classLoader);
    }

    @Override
    public void connect() {
        //NOOP
    }

    @Override
    public InputStream getInputStream() throws IOException {
        String location = url.getAuthority() + url.getPath();
        InputStream is = classLoader.getResourceAsStream(location);
        if(is == null)
            throw new IOException("Classpath resource [" + location + "] not found");
        return is;
    }
}