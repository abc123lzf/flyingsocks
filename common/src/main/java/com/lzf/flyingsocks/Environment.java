package com.lzf.flyingsocks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 系统环境相关API
 */
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
    default int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * 根据路径分隔符判断当前操作系统是否是Windows
     * @return 当前系统是否是Windows系统
     */
    default boolean isWindows() {
        return File.separatorChar == '\\';
    }
}
