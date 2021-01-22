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
package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.server.Server;
import com.lzf.flyingsocks.server.ServerConfig;
import com.lzf.flyingsocks.server.core.client.ClientProcessor;

import com.lzf.flyingsocks.server.core.dispatch.DispatchProceessor;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyProcessor extends AbstractComponent<Server> implements ProxyTaskManager {

    private static final AtomicInteger ID_BUILDER = new AtomicInteger(0);

    //ProxyProcessor ID
    private final int handlerId;

    //Boss线程
    private final EventLoopGroup bossWorker;

    //Worker线程池
    private final EventLoopGroup childWorker;

    //配置信息
    private final ServerConfig.Node serverConfig;

    //代理任务订阅者列表
    private final CopyOnWriteArrayList<ProxyTaskSubscriber> proxyTaskSubscribers = new CopyOnWriteArrayList<>();


    public ProxyProcessor(Server server, ServerConfig.Node serverConfig) {
        super(serverConfig.name, server);
        this.handlerId = ID_BUILDER.incrementAndGet();
        this.serverConfig = serverConfig;

        int cpus = parent.availableProcessors();
        int bossWorkerCount = cpus <= 4 ? 1 : 2;
        int childWorkerCount = cpus * 2;
        boolean linux = !parent.isMacOS() && !parent.isWindows();
        EventLoopGroup bossWorker;
        EventLoopGroup childWorker;
        if (linux) {
            try {
                bossWorker = new EpollEventLoopGroup(bossWorkerCount);
                childWorker = new EpollEventLoopGroup(childWorkerCount);
            } catch (Throwable t) {
                log.info("Unable to create EpollEventLoopGroup [{}]", t.getMessage());
                try {
                    bossWorker = new KQueueEventLoopGroup(bossWorkerCount);
                    childWorker = new KQueueEventLoopGroup(childWorkerCount);
                } catch (Throwable t2) {
                    log.info("Unable to create KQueueEventLoopGroup [{}]", t2.getMessage());
                    bossWorker = new NioEventLoopGroup(bossWorkerCount);
                    childWorker = new NioEventLoopGroup(childWorkerCount);
                }
            }
        } else {
            bossWorker = new NioEventLoopGroup(bossWorkerCount);
            childWorker = new NioEventLoopGroup(childWorkerCount);
        }

        this.bossWorker = bossWorker;
        this.childWorker = childWorker;
    }

    @Override
    protected void initInternal() {
        addComponent(new ClientProcessor(this));
        addComponent(new DispatchProceessor(this));
        super.initInternal();
    }

    @Override
    protected void stopInternal() {
        bossWorker.shutdownGracefully();
        childWorker.shutdownGracefully();
        super.stopInternal();
    }

    public final int getHandlerId() {
        return handlerId;
    }

    /**
     * @return 该代理处理器绑定的端口
     */
    public final int getPort() {
        return serverConfig.port;
    }

    /**
     * @return 收发CA证书的端口
     */
    public final int getCertPort() {
        return serverConfig.certPort;
    }

    /**
     * @return 该代理处理器最大的客户端TCP连接数
     */
    public final int getMaxClient() {
        return serverConfig.maxClient;
    }

    /**
     * @return 连接请求处理线程池
     */
    public final EventLoopGroup getChildWorker() {
        return childWorker;
    }

    /**
     * @return 连接接收线程池
     */
    public final EventLoopGroup getBossWorker() {
        return bossWorker;
    }


    @Override
    public void registerSubscriber(ProxyTaskSubscriber subscriber) {
        proxyTaskSubscribers.add(subscriber);
        if (log.isInfoEnabled())
            log.info("ProxyTaskSubscriber {} has been register in manager.", subscriber.toString());
    }

    @Override
    public void removeSubscriber(ProxyTaskSubscriber subscriber) {
        if (proxyTaskSubscribers.remove(subscriber)) {
            if (log.isInfoEnabled())
                log.info("ProxyTaskSubscriber {} has been remove from manager.", subscriber.toString());
        } else if (log.isWarnEnabled()) {
            log.warn("Remove failure, cause ProxyTaskSubscriber doesn't found in list.");
        }
    }

    @Override
    public void publish(ProxyTask task) {
        if (proxyTaskSubscribers.size() == 0) {
            log.error("No ProxyTaskSubscriber register.");
            task.getRequestMessage().getMessage().release();
            return;
        }

        int hash = Math.abs(task.hashCode());
        int size = proxyTaskSubscribers.size();
        try {
            proxyTaskSubscribers.get(hash % size).receive(task);
        } catch (IndexOutOfBoundsException e) {
            log.error("ProxyTaskManager status error.", e);
            task.getRequestMessage().getMessage().release();
        }
    }
    /**
     * 获取当前ProxyHandler的配置信息
     *
     * @return 配置信息
     */
    public ServerConfig.Node getServerConfig() {
        return serverConfig;
    }

}
