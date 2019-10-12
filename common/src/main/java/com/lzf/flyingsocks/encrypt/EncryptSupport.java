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
     * @param name 加密方式
     * @return EncryptProvider对象
     */
    public static EncryptProvider lookupProvider(String name) {
        Class<? extends EncryptProvider> provider = providers.get(name.toLowerCase());
        if(provider != null) {
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
        if(p.getClass() == type) {
            return (T) p;
        } else {
            throw new ClassCastException();
        }
    }


    private EncryptSupport() {
        throw new UnsupportedOperationException();
    }

}
