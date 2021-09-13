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
package com.lzf.flyingsocks.server.core.client;

import com.lzf.flyingsocks.protocol.AuthRequestMessage;
import com.lzf.flyingsocks.server.core.ClientSession;
import com.lzf.flyingsocks.server.core.ProxyTaskManager;
import io.netty.channel.Channel;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.util.concurrent.FastThreadLocal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 用于保存每个客户端连接所需要的参数
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/17 21:42
 */
final class ConnectionContext {

    private static final FastThreadLocal<Map<Channel, ConnectionContext>> CONTEXT = new FastThreadLocal<Map<Channel, ConnectionContext>>() {
        @Override
        protected Map<Channel, ConnectionContext> initialValue() {
            return new HashMap<>();
        }
    };

    /**
     * 客户端Session
     */
    private ClientSession clientSession;

    /**
     * 用于发布代理任务
     */
    private ProxyTaskManager proxyTaskManager;

    /**
     * 认证逻辑
     */
    private Predicate<AuthRequestMessage> authPredicate;

    /**
     * DNS客户端
     */
    private DnsNameResolver nameResolver;


    private ConnectionContext() {
    }

    /**
     * 初始化ConnectionContext
     *
     * @param channel 客户端与服务端连接的 {@link io.netty.channel.socket.SocketChannel}
     * @param proxyTaskManager 发布代理任务 {@link com.lzf.flyingsocks.server.core.ProxyProcessor}
     * @param authPredicate 认证逻辑 {@link com.lzf.flyingsocks.server.core.client.ClientProcessor#doAuth(AuthRequestMessage)}
     */
    static void initial(Channel channel, ProxyTaskManager proxyTaskManager, Predicate<AuthRequestMessage> authPredicate,
                        DnsNameResolver nameResolver) {
        accessCheckout(channel);
        ConnectionContext ctx = new ConnectionContext();
        ctx.proxyTaskManager = Objects.requireNonNull(proxyTaskManager);
        ctx.authPredicate = Objects.requireNonNull(authPredicate);
        ctx.nameResolver = Objects.requireNonNull(nameResolver);

        Map<Channel, ConnectionContext> map = CONTEXT.get();
        map.put(channel, ctx);

        channel.closeFuture().addListener(future -> map.remove(channel));
    }


    static ClientSession clientSession(Channel channel) {
        accessCheckout(channel);
        Map<Channel, ConnectionContext> map = CONTEXT.get();
        ConnectionContext ctx = map.get(channel);
        if (ctx == null) {
            ctx = new ConnectionContext();
            map.put(channel, ctx);
        }

        return ctx.clientSession;
    }

    static void putClientSession(Channel channel, ClientSession clientSession) {
        accessCheckout(channel);
        Objects.requireNonNull(clientSession);
        Map<Channel, ConnectionContext> map = CONTEXT.get();
        ConnectionContext ctx = map.get(channel);
        if (ctx == null) {
            throw new IllegalStateException("Not initialize");
        }

        ctx.clientSession = clientSession;
    }

    static ProxyTaskManager proxyTaskManager(Channel channel) {
        accessCheckout(channel);
        Map<Channel, ConnectionContext> map = CONTEXT.get();
        ConnectionContext ctx = map.get(channel);
        if (ctx == null) {
            ctx = new ConnectionContext();
            map.put(channel, ctx);
        }

        return ctx.proxyTaskManager;
    }

    static Predicate<AuthRequestMessage> authPredicate(Channel channel) {
        accessCheckout(channel);
        Map<Channel, ConnectionContext> map = CONTEXT.get();
        ConnectionContext ctx = map.get(channel);
        if (ctx == null) {
            ctx = new ConnectionContext();
            map.put(channel, ctx);
        }

        return ctx.authPredicate;
    }


    static DnsNameResolver nameResolver(Channel channel) {
        accessCheckout(channel);
        Map<Channel, ConnectionContext> map = CONTEXT.get();
        ConnectionContext ctx = map.get(channel);
        if (ctx == null) {
            ctx = new ConnectionContext();
            map.put(channel, ctx);
        }

        return ctx.nameResolver;
    }


    private static void accessCheckout(Channel channel) {
        if (!channel.eventLoop().inEventLoop(Thread.currentThread())) {
            throw new IllegalStateException();
        }
    }
}
