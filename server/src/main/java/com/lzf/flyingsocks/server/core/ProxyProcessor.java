package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.server.Server;
import com.lzf.flyingsocks.server.ServerConfig;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.FastThreadLocal;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyProcessor extends AbstractComponent<Server> {

    private static final AtomicInteger ID_BUILDER = new AtomicInteger(0);

    private final int handlerId;

    private final Map<Channel, ClientSession> activeClientMap = new ConcurrentHashMap<>();

    private EventLoopGroup connectionReceiveWorker = new NioEventLoopGroup(1);

    private EventLoopGroup requestProcessWorker = new NioEventLoopGroup();

    private final int port;

    private final int maxClient;

    private final ChannelInboundHandler clientSessionHandler = new ClientSessionHandler();

    private final ServerConfig.Node serverConfig;

    private BlockingQueue<ProxyTask> proxyTaskQueue = new LinkedBlockingQueue<>();

    FastThreadLocal<Map<Channel, ClientSession>> localClientMap = new FastThreadLocal<Map<Channel, ClientSession>>() {
        @Override
        protected Map<Channel, ClientSession> initialValue() {
            return new WeakHashMap<>();
        }
    };

    public ProxyProcessor(Server server, ServerConfig.Node serverConfig) {
        super(serverConfig.name, server);
        this.handlerId = ID_BUILDER.incrementAndGet();
        this.port = serverConfig.port;
        this.maxClient = serverConfig.maxClient;
        this.serverConfig = serverConfig;
    }

    @Override
    protected void initInternal() {
        addComponent(new ClientProcessor(this));
        addComponent(new DispatchProceessor(this));
        super.initInternal();
    }

    @Override
    protected void stopInternal() {
        connectionReceiveWorker.shutdownGracefully();
        requestProcessWorker.shutdownGracefully();
        activeClientMap.clear();
        super.stopInternal();
    }

    public final int getHandlerId() {
        return handlerId;
    }

    /**
     * @return 该代理处理器绑定的端口
     */
    public final int getPort() {
        return port;
    }

    /**
     * @return 该代理处理器最大的客户端TCP连接数
     */
    public final int getMaxClient() {
        return maxClient;
    }

    void pushProxyTask(ProxyTask proxyTask) throws InterruptedException {
        proxyTaskQueue.put(proxyTask);
    }

    ProxyTask pollProxyTask() throws InterruptedException {
        return proxyTaskQueue.take();
    }

    /**
     * @return 连接请求处理线程池
     */
    final EventLoopGroup getRequestProcessWorker() {
        return requestProcessWorker;
    }

    /**
     * @return 连接接收线程池
     */
    final EventLoopGroup getConnectionReceiveWorker() {
        return connectionReceiveWorker;
    }

    /**
     * @param clientSession 客户端会话对象
     */
    private void putClientSession(ClientSession clientSession) {
        activeClientMap.put(clientSession.socketChannel(), clientSession);
    }

    /**
     * 根据Channel获取客户端会话对象
     * @param channel Channel通道
     * @return 客户端会话
     */
    final ClientSession getClientSession(Channel channel) {
        return activeClientMap.get(channel);
    }

    private void removeClientSession(Channel channel) {
        activeClientMap.remove(channel);
    }

    /**
     * 获取默认的会话管理器
     * @return 会话管理器
     */
    public ChannelInboundHandler clientSessionHandler() {
        return clientSessionHandler;
    }

    /**
     * 获取当前ProxyHandler的配置信息
     * @return 配置信息
     */
    ServerConfig.Node getServerConfig() {
        return serverConfig;
    }



    @ChannelHandler.Sharable
    private final class ClientSessionHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            localClientMap.get().get(ctx.channel()).updateLastActiveTime();
            ctx.fireChannelRead(msg);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ClientSession state = new ClientSession(ctx.channel());
            putClientSession(state);
            localClientMap.get().put(state.socketChannel(), state);

            ctx.fireChannelActive();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(log.isWarnEnabled())
                log.warn("An exception occur", cause);
            ctx.close().addListener(future -> {
                removeClientSession(ctx.channel());
            });
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            localClientMap.get().remove(ctx.channel());
            removeClientSession(ctx.channel());

            ctx.fireChannelInactive();
        }
    }


}
