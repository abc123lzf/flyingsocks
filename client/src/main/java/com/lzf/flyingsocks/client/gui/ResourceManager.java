package com.lzf.flyingsocks.client.gui;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author lizifan lzf@webull.com
 * @create 2019.8.13 11:26
 * @description GUI资源管理器
 */
public final class ResourceManager {

    static {
        try {
            //加载这个类以便支持‘classpath:’类型的URL
            Class.forName("com.lzf.flyingsocks.url.ClasspathURLHandlerFactory");
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    public static InputStream openIconImageStream() throws IOException {
        return new URL("classpath://icon.png").openStream();
    }

    public static InputStream openSystemTrayImageStream() throws IOException {
        return new URL("classpath://icon-tray.png").openStream();
    }

    public static Image loadSystemTrayImage() throws IOException {
        return ImageIO.read(new URL("classpath://icon.png"));
    }

    public static Image loadIconImage() throws IOException {
        return ImageIO.read(new URL("classpath://icon-tray.png"));
    }

    private ResourceManager() {
        throw new UnsupportedOperationException();
    }

}
