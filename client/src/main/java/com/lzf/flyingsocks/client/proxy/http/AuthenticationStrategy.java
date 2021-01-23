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
package com.lzf.flyingsocks.client.proxy.http;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;

/**
 * HTTP代理认证逻辑
 * 基于请求头proxy-authorization实现
 * 具体规范参考<a href="https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Proxy-Authorization" />
 *
 * @see HttpHeaderNames#PROXY_AUTHORIZATION
 * @author lzf abc123lzf@126.com
 * @since 2021/1/23 1:00
 */
interface AuthenticationStrategy {

    /**
     * 认证未通过或者未发送认证信息时响应头包含的proxy-authenticate字段
     *
     * @return proxy-authenticate字段值
     */
    String proxyAuthenticateHeader();

    /**
     * 执行认证
     *
     * @param type 认证类型
     * @param credentials 认证凭据
     * @return 认证是否通过
     */
    boolean grantAuthorization(String type, String credentials);


    /**
     * 认证成功后对{@link ChannelPipeline}的操作
     *
     * @param pipeline 当前HTTP代理连接管道对象
     * @param type 认证类型
     * @param credentials 认证凭据
     */
    default void afterAuthorizationSuccess(ChannelPipeline pipeline, String type, String credentials) { }

}

/**
 * 固定用户名和密码
 */
class SimpleAuthenticationStrategy implements AuthenticationStrategy {

    private final String username;

    private final String password;

    public SimpleAuthenticationStrategy(String username, String password) {
        if (StringUtils.isAnyBlank(username, password)) {
            throw new IllegalArgumentException();
        }

        this.username = username;
        this.password = password;
    }


    @Override
    public String proxyAuthenticateHeader() {
        return "Basic realm=\"flyingsocks HTTP Proxy service\"";
    }


    @Override
    public boolean grantAuthorization(String type, String credentials) {
        if (StringUtils.isBlank(type) || StringUtils.isBlank(credentials)) {
            return false;
        }

        if (!type.equals("Basic")) {
            return false;
        }

        try {
            String[] arr = StringUtils.split(new String(Base64.getDecoder().decode(credentials)), ':');
            if (arr.length != 2) {
                return false;
            }

            return this.username.equals(arr[0]) && this.password.equals(arr[1]);
        } catch (RuntimeException e) {
            return false;
        }
    }
}