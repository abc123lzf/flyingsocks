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
package com.lzf.flyingsocks.client.proxy;

import java.util.EnumSet;
import java.util.Set;

import static com.lzf.flyingsocks.client.proxy.ProxyRequest.Protocol;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;

/**
 * 代理请求消息订阅者
 */
public interface ProxyRequestSubscriber {

    /**
     * 包括{@link Protocol#TCP}和{@link Protocol#UDP}协议
     */
    Set<Protocol> ANY = unmodifiableSet(EnumSet.allOf(Protocol.class));

    /**
     * 仅限{@link Protocol#TCP}协议
     */
    Set<Protocol> ONLY_TCP = singleton(Protocol.TCP);

    /**
     * 仅限{@link Protocol#UDP}协议
     */
    Set<Protocol> ONLY_UDP = singleton(Protocol.UDP);

    /**
     * 接收消息
     *
     * @param request ProxyRequest请求
     */
    void receive(ProxyRequest request);

    /**
     * @return 是否接收需要代理的消息(即经过PAC判定需要代理的消息)
     */
    default boolean receiveNeedProxy() {
        return false;
    }

    /**
     * @return 是否接收无需代理的消息(即经过PAC判定无需代理的消息)
     */
    default boolean receiveNeedlessProxy() {
        return false;
    }

    /**
     * @return 可以接收的代理协议Set
     */
    default Set<Protocol> requestProtocol() {
        return emptySet();
    }
}