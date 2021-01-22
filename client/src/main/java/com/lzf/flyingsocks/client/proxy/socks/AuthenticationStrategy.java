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
package com.lzf.flyingsocks.client.proxy.socks;

import io.netty.channel.ChannelPipeline;
import org.apache.commons.lang3.StringUtils;

/**
 * Socks5 客户端认证策略
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/22 15:20
 */
interface AuthenticationStrategy {

    /**
     * 执行认证
     *
     * @param username 用户名
     * @param password 密码
     * @return 认证是否通过
     */
    boolean grantAuthorization(String username, String password);

    /**
     * 认证通过后对管道的操作
     * 目前预留，方便日后扩展
     *
     * @param pipeline 当前Socks5客户端连接管道对象
     * @param username 用户名
     */
    default void afterAuthorizationSuccess(ChannelPipeline pipeline, String username) { }

}


/**
 * 简易认证策略，使用固定的用户名和密码
 */
class SimpleAuthenticationStrategy implements AuthenticationStrategy {

    private final String username;

    private final String password;

    SimpleAuthenticationStrategy(String username, String password) {
        if (StringUtils.isAnyBlank(username, password)) {
            throw new IllegalArgumentException("Username or password is blank");
        }

        this.username = username;
        this.password = password;
    }

    @Override
    public boolean grantAuthorization(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }
}