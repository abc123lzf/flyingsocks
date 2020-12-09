package com.lzf.flyingsocks.client.gui;

import io.netty.util.internal.PlatformDependent;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author lizifan 695199262@qq.com
 * @since  2019.8.13 11:26
 * GUI资源管理器
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

    public static InputStream openFlyingsocksImageStream() throws IOException {
        return new URL("classpath://flyingsocks.png").openStream();
    }

    public static InputStream openIconImageStream() throws IOException {
        return new URL("classpath://icon.png").openStream();
    }

    public static InputStream openSystemTrayImageStream() throws IOException {
        if (PlatformDependent.isOsx()) {
            return new URL("classpath://icon-tray-mac.png").openStream();
        }

        return new URL("classpath://icon-tray.png").openStream();
    }

    public static InputStream openSaveIconImageStream() throws IOException {
        return new URL("classpath://save-icon.png").openStream();
    }

    public static InputStream openDeleteIconImageStream() throws IOException {
        return new URL("classpath://delete-icon.png").openStream();
    }

    @Deprecated
    public static Image loadSystemTrayImage() throws IOException {
        return ImageIO.read(new URL("classpath://icon.png"));
    }

    @Deprecated
    public static Image loadIconImage() throws IOException {
        return ImageIO.read(new URL("classpath://icon-tray.png"));
    }


    private ResourceManager() {
        throw new UnsupportedOperationException();
    }

}
