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
package com.lzf.flyingsocks.client.gui;

import io.netty.util.internal.PlatformDependent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author lizifan 695199262@qq.com
 * @since 2019.8.13 11:26
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

    public static InputStream openIconImageStream() throws IOException {
        return new URL("classpath://META-INF/ui-resource/icon.png").openStream();
    }

    public static InputStream openSystemTrayImageStream() throws IOException {
        if (PlatformDependent.isOsx()) {
            return new URL("classpath://META-INF/ui-resource/icon-tray-mac.png").openStream();
        }

        return new URL("classpath://META-INF/ui-resource/icon-tray.png").openStream();
    }

    public static InputStream openSaveIconImageStream() throws IOException {
        return new URL("classpath://META-INF/ui-resource/save-icon.png").openStream();
    }

    public static InputStream openDeleteIconImageStream() throws IOException {
        return new URL("classpath://META-INF/ui-resource/delete-icon.png").openStream();
    }

    private ResourceManager() {
        throw new UnsupportedOperationException();
    }

}
