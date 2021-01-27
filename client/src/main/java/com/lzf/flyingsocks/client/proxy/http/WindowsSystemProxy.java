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
package com.lzf.flyingsocks.client.proxy.http;

import io.netty.util.internal.NativeLibraryLoader;
import io.netty.util.internal.PlatformDependent;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 该Class会读写Windows注册表 HKEY_CURRENT_USER\SOFTWARE\Microsoft\Windows\CurrentVersion\Internet Settings
 * 下的ProxyEnable/ProxyServer/ProxyOverride数据项，从而实现系统代理
 *
 * 本地方法源码：src/main/c/windows-system-proxy
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/27 21:29
 */
final class WindowsSystemProxy {

    private static final Throwable UNAVAILABILITY_CAUSE;

    static {
        if (!PlatformDependent.isWindows()) {
            UNAVAILABILITY_CAUSE = new UnsatisfiedLinkError("Platform is not windows!");
        } else {
            Throwable error;
            try {
                String libName = "libflyingsocks_systemproxy_windows_" + PlatformDependent.normalizedArch();
                ClassLoader loader = PlatformDependent.getClassLoader(WindowsSystemProxy.class);
                NativeLibraryLoader.load(libName, loader);
                error = null;
            } catch (Throwable t) {
                error = t;
            }

            UNAVAILABILITY_CAUSE = error;
        }
    }


    static boolean isAvailable() {
        return UNAVAILABILITY_CAUSE == null;
    }


    static Throwable unavailabilityCause() {
        return UNAVAILABILITY_CAUSE;
    }

    /**
     * @return 系统代理是否打开
     */
    static boolean isProxyEnable() {
        return isProxyEnable0();
    }

    /**
     * 设置系统代理开关
     * @param enable 开关
     * @return 是否设置成功
     */
    static boolean switchProxy(boolean enable) {
        return switchSystemProxy0(enable);
    }

    /**
     * 设置代理服务器地址
     * @param host 服务器地址
     * @param port 端口
     * @return 是否设置成功
     */
    static boolean setupProxyServerAddress(String host, int port) {
        Objects.requireNonNull(host);
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port:" + port);
        }

        return setupProxyServerAddress0(host + ":" + port);
    }

    /**
     * 设置不代理的主机
     * @param list 主机列表
     * @return 是否设置成功
     */
    static boolean setupNoProxyHosts(List<String> list) {
        Objects.requireNonNull(list);
        int size = list.size();
        int i = 0;
        StringBuilder sb = new StringBuilder(size * 10);
        for (String host : list) {
            sb.append(host);
            if (i < size - 1) {
                sb.append(';');
            }
            i++;
        }

        return setupNoProxyHosts0(sb.toString());
    }


    /**
     * @return 不代理的主机名列表
     */
    static List<String> obtainNoProxyHosts() {
        String result = obtainNoProxyHosts0();
        String[] arr = StringUtils.split(result, ';');
        return Stream.of(arr).collect(Collectors.toList());
    }


    /**
     * ProxyEnable值是否不为0
     */
    private static native boolean isProxyEnable0();

    /**
     * 修改ProxyEnable
     */
    private static native boolean switchSystemProxy0(boolean enable);

    /**
     * 修改ProxyServer
     */
    private static native boolean setupProxyServerAddress0(String address);

    /**
     * 修改ProxyOverride
     */
    private static native boolean setupNoProxyHosts0(String hostList);

    /**
     * 获取ProxyOverride数据
     */
    private static native String obtainNoProxyHosts0();


    private WindowsSystemProxy() {
        super();
    }

    public static void main(String[] args) {
        System.out.println(switchProxy(false));
    }
}
