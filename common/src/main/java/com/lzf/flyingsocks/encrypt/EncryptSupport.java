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
package com.lzf.flyingsocks.encrypt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("all")
public abstract class EncryptSupport {

    private static final Map<String, Class<? extends EncryptProvider>> providers = new ConcurrentHashMap<>(8);

    private static void registerEncryptProvider(Class<? extends EncryptProvider> clazz) throws Exception {
        EncryptProvider instance = clazz.newInstance();
        providers.put(instance.getName().toLowerCase(), clazz);
    }

    static {
        try {
            registerEncryptProvider(JksSSLEncryptProvider.class);
            registerEncryptProvider(OpenSSLEncryptProvider.class);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * 查找对应的加密Handler提供对象
     *
     * @param name 加密方式
     * @return EncryptProvider对象
     */
    public static EncryptProvider lookupProvider(String name) {
        Class<? extends EncryptProvider> provider = providers.get(name.toLowerCase());
        if (provider != null) {
            try {
                return provider.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new Error(e); //Should not be happened.
            }
        } else {
            throw new IllegalArgumentException("EncryptProvider " + name + " not found");
        }
    }

    public static <T extends EncryptProvider> T lookupProvider(String name, Class<T> type) {
        EncryptProvider p = lookupProvider(name);
        if (p.getClass() == type) {
            return (T) p;
        } else {
            throw new ClassCastException();
        }
    }


    private EncryptSupport() {
        throw new UnsupportedOperationException();
    }

}
