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
package com.lzf.flyingsocks.server.db;

/**
 * 用于实现用户认证方式
 */
public interface UserDatabase {

    /**
     * 执行认证
     *
     * @param group    用户组名
     * @param username 用户名
     * @param password 密码
     * @return 是否认证成功
     */
    boolean doAuth(String group, String username, String password);

    /**
     * 注册用户
     *
     * @param group    用户组名
     * @param username 用户名
     * @param password 密码
     * @return 是否认证成功
     */
    boolean register(String group, String username, String password);

    /**
     * 删除用户
     *
     * @param group    用户组名
     * @param username 用户名
     * @return 是否删除成功
     */
    boolean delete(String group, String username);

    /**
     * 修改用户密码
     *
     * @param group       用户组名
     * @param username    用户名
     * @param newPassword 新密码
     * @return 是否修改成功
     */
    boolean changePassword(String group, String username, String newPassword);

    /**
     * 用户组
     */
    interface UserGroup {

        /**
         * @return 用户组名
         */
        String name();
    }
}
