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
package com.lzf.flyingsocks.encrypt;

import com.lzf.flyingsocks.Named;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;

import java.util.Map;

/**
 * 加密、解密Handler提供者
 * 对flyingsocks客户端和服务端传输的数据进行加密
 */
public interface EncryptProvider extends Named {

    /**
     * 初始化Provider
     *
     * @param params 初始化参数
     */
    void initialize(Map<String, ?> params) throws Exception;

    /**
     * @return 返回decodeHandler方法和encodeHandler返回的是不是同一个类型
     */
    boolean isInboundHandlerSameAsOutboundHandler();

    /**
     * 返回解密处理器
     *
     * @param params 参数
     * @return ChannelInboundHandler实例
     * @throws Exception 实例化过程抛出的异常
     */
    ChannelInboundHandler decodeHandler(Map<String, ?> params) throws Exception;

    /**
     * 返回加密处理器
     *
     * @param params 参数
     * @return ChannelOutboundHandler实例
     * @throws Exception 实例化过程抛出的一场
     */
    ChannelOutboundHandler encodeHandler(Map<String, ?> params) throws Exception;
}
