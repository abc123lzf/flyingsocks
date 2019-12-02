package com.lzf.flyingsocks.client.proxy;

public interface ProxyAutoChecker {

    /**
     * 不代理
     */
    int PROXY_NO = 0;

    /**
     * GFW黑名单代理模式
     */
    int PROXY_GFW_LIST = 1;

    /**
     * 全局代理模式
     */
    int PROXY_GLOBAL = 2;

    /**
     * IP白名单模式
     */
    int PROXY_NON_CN = 3;

    /**
     * 判断是否需要代理
     * @param host 主机名或者IPv4/IPv6地址
     * @return 是否需要代理
     */
    boolean needProxy(String host);

    /**
     * @return 当前代理模式
     */
    int proxyMode();

    /**
     * 改变代理模式
     * @param mode 模式编号
     */
    void changeProxyMode(int mode);

}
