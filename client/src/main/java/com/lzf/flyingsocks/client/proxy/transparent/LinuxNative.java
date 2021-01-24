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
package com.lzf.flyingsocks.client.proxy.transparent;

import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.util.internal.NativeLibraryLoader;
import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * 透明代理本地接口
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/24 21:08
 */
final class LinuxNative {

    private static final Logger log = LoggerFactory.getLogger(LinuxNative.class);

    static {
        String libName = "libflyingsocks_transparent_" + PlatformDependent.normalizedArch();
        log.info("Load transparent native lib: {}", libName);

        ClassLoader loader = PlatformDependent.getClassLoader(LinuxNative.class);
        try {
            NativeLibraryLoader.load(libName, loader);
        } catch (UnsatisfiedLinkError e) {
            log.error("Load transparent native lib failure", e);
            throw e;
        }
    }

    /**
     * @return 目标地址
     */
    public static InetSocketAddress getTargetAddress(EpollSocketChannel channel) {
        Objects.requireNonNull(channel);
        return getTargetAddress0(channel.fd().intValue());
    }


    private native static InetSocketAddress getTargetAddress0(int fd);


    private LinuxNative() { }
}
