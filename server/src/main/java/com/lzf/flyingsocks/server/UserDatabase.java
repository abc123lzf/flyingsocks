package com.lzf.flyingsocks.server;

public interface UserDatabase {

    /**
     * 执行认证
     * @param group 用户组名
     * @param username 用户名
     * @param password 密码
     * @return 是否认证成功
     */
    boolean doAuth(String group, String username, String password);

    /**
     * 注册用户
     * @param group 用户组名
     * @param username 用户名
     * @param password 密码
     * @return 是否认证成功
     */
    boolean register(String group, String username, String password);

    /**
     * 删除用户
     * @param group 用户组名
     * @param username 用户名
     * @return 是否删除成功
     */
    boolean delete(String group, String username);

    /**
     * 修改用户密码
     * @param group 用户组名
     * @param username 用户名
     * @param newPassword 新密码
     * @return 是否修改成功
     */
    boolean changePassword(String group, String username, String newPassword);
}
