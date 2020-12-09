package com.lzf.flyingsocks.management.service;

public interface UserService {

    /**
     * 验证用户名密码是否正确
     * @param username 用户名
     * @param password 密码
     * @return 是否正确
     */
    boolean verify(String username, String password);

    /**
     * 记录登录历史
     *
     * @param username 用户名
     */
    void recordLoginHistory(String username, String ipAddress);

}
