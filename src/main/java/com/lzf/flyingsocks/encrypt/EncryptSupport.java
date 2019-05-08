package com.lzf.flyingsocks.encrypt;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
public abstract class EncryptSupport {

    private static final Map<String, Class<? extends EncryptProvider>> providers = new ConcurrentHashMap<>(8);

    private static void registerEncryptProvider(Class<? extends EncryptProvider> clazz) throws Exception {
        EncryptProvider instance = clazz.newInstance();
        providers.put(instance.getName().toLowerCase(), clazz);
    }

    static {
        ClassLoader cl = EncryptSupport.class.getClassLoader();
        Package p = EncryptSupport.class.getPackage();
        try {
            Enumeration<URL> it = cl.getResources(p.getName().replace('.', '/'));
            while(it.hasMoreElements()) {
                URL url = it.nextElement();
                String name = (url.getAuthority() + url.getContent()).replace('/', '.');
                try {
                    Class<?> clazz = cl.loadClass(name);
                    if(clazz.isAssignableFrom(EncryptProvider.class)) {
                        Class<? extends EncryptProvider> klass = (Class) clazz;
                        try {
                            registerEncryptProvider(klass);
                        } catch (Exception e) {
                            throw new Error(e);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw new Error(e);
                }
            }

        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * 查找对应的加密Handler提供对象
     * @param name 加密方式
     * @return EncryptProvider对象
     * @throws Exception
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


    private EncryptSupport() {
        throw new UnsupportedOperationException();
    }

}
