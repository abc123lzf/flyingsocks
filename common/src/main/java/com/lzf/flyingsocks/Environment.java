package com.lzf.flyingsocks;

import java.io.IOException;
import java.io.InputStream;

public interface Environment {

    /**
     * 获取环境变量
     * @param key 键值
     * @return 环境变量值
     */
    String getEnvironmentVariable(String key);

    /**
     * 获取系统变量
     * @param key 键值
     * @return 系统变量值
     */
    String getSystemProperties(String key);

    /**
     * 设置系统变量
     * @param key 变量名
     * @param value 变量值
     */
    void setSystemProperties(String key, String value);

    /**
     * 获取资源
     * @param path 路径
     * @return 输入流
     */
    InputStream loadResource(String path) throws IOException;

    /**
     * 获取可用的处理器数量
     * @return
     */
    default int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }
}
