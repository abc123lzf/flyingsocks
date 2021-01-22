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
package com.lzf.flyingsocks;

import io.netty.util.internal.PlatformDependent;

import java.io.IOException;
import java.io.InputStream;

/**
 * 系统环境相关API
 */
public interface Environment {

    /**
     * 获取环境变量
     *
     * @param key 键值
     * @return 环境变量值
     */
    String getEnvironmentVariable(String key);

    /**
     * 获取系统变量
     *
     * @param key 键值
     * @return 系统变量值
     */
    String getSystemProperties(String key);

    /**
     * 设置系统变量
     *
     * @param key   变量名
     * @param value 变量值
     */
    void setSystemProperties(String key, String value);

    /**
     * 获取资源
     *
     * @param path 路径
     * @return 输入流
     */
    InputStream loadResource(String path) throws IOException;

    /**
     * @return 可用的处理器数量
     */
    default int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * 根据Netty的平台依赖类PlatformDependent获取当前环境是否是Windows
     *
     * @return 当前系统是否是Windows系统
     */
    default boolean isWindows() {
        return PlatformDependent.isWindows();
    }

    /**
     * @return 当前系统是否是MacOS / OS X
     */
    default boolean isMacOS() {
        return PlatformDependent.isOsx();
    }
}
