package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.GlobalConfig;
import com.lzf.flyingsocks.encrypt.EncryptProvider;
import com.lzf.flyingsocks.encrypt.EncryptSupport;
import com.lzf.flyingsocks.encrypt.OpenSSLEncryptProvider;
import com.lzf.flyingsocks.protocol.*;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.*;
import java.util.concurrent.*;

/**
 * flyingsocks服务器的连接管理组件，每个ProxyServerComponent对象代表一个服务器节点
 * 例如：若需要连接多个flyingsocks服务器实现负载均衡，则需要多个ProxyServerComponent对象
 *
 */
public class ProxyServerComponent extends AbstractComponent<ProxyComponent> implements ProxyRequestSubscriber {
    private static final int DEFAULT_PROCESSOR_THREAD = 4;

    //该服务器节点配置信息
    private final ProxyServerConfig.Node config;

    //客户端是否启动了这个节点
    private volatile boolean use;

    //该节点连接状态是否活跃
    private volatile boolean active = false;

    //连接线程池
    private volatile EventLoopGroup loopGroup;

    //通用Netty引导对象
    private volatile Bootstrap bootstrap;

    //用于处理客户端消息
    private volatile ExecutorService clientMessageProcessor;

    //与flyingsocks服务器会话对象
    private volatile ProxyServerSession proxyServerSession;

    //代理请求消息队列
    private final BlockingQueue<ProxyRequest> proxyRequestQueue = new LinkedBlockingQueue<>();

    //活跃的代理请求Map
    private final Map<String, ProxyRequest> activeProxyRequestMap = new ConcurrentHashMap<>(512);

    //确保可以正式向服务器发送代理请求后释放clientMessageProcessor中的等待线程
    private volatile CountDownLatch taskWaitLatch;


    public ProxyServerComponent(ProxyComponent proxyComponent, ProxyServerConfig.Node config) {
        super(generalName(config.getHost(), config.getPort()), Objects.requireNonNull(proxyComponent));
        this.config = Objects.requireNonNull(config);
        this.use = config.isUse();
    }

    @Override
    protected void initInternal() {
        EncryptProvider provider;
        //目前仅支持OpenSSL加密和不加密(测试性质)
        if(config.getEncryptType() == ProxyServerConfig.EncryptType.NONE) {
            provider = null;
        } else if(config.getEncryptType() == ProxyServerConfig.EncryptType.SSL) {
            ConfigManager<?> manager = parent.getParentComponent().getConfigManager();
            OpenSSLConfig sslcfg = new OpenSSLConfig(manager, config.getHost());
            manager.registerConfig(sslcfg);

            provider = EncryptSupport.lookupProvider("OpenSSL", OpenSSLEncryptProvider.class);
            Map<String, Object> params = new HashMap<>();
            params.put("client", true);
            params.put("file.cert.root", sslcfg.openRootCertStream());

            try {
                provider.initialize(params);
            } catch (Exception e) {
                throw new ComponentException(e);
            }
        } else {
            throw new ComponentException("Unsupport encrypt type " + config.getEncryptType());
        }

        ConfigManager<?> cm = parent.getParentComponent().getConfigManager();
        GlobalConfig cfg = cm.getConfig(GlobalConfig.NAME, GlobalConfig.class);

        taskWaitLatch = new CountDownLatch(1);

        loopGroup = new NioEventLoopGroup(2);
        bootstrap = new Bootstrap().group(loopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, cfg.getConnectionTimeout())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline cp = ch.pipeline();
                        Map<String, Object> m = new HashMap<>(2);
                        m.put("alloc", ch.alloc());
                        if(provider != null) {
                            if(!provider.isInboundHandlerSameAsOutboundHandler())
                                cp.addLast(provider.encodeHandler(m));
                            cp.addLast(provider.decodeHandler(m));
                        }
                        cp.addLast(new InitialHandler());
                    }
                });

        clientMessageProcessor = createMessageExecutor();

        super.initInternal();
    }

    @Override
    protected void startInternal() {
        if(use) {
            doConnect(true);
            /*if(!active) {
                stop();
                return;
            }*/
        }

        ConfigManager<?> cm = parent.getParentComponent().getConfigManager();
        cm.registerConfigEventListener(new ConfigRemovedListener());

        super.startInternal();

        if(active)
            getParentComponent().addActiveProxyServer(this);
    }

    @Override
    public boolean receiveNeedProxy() {
        return true;
    }

    @Override
    public boolean receiveNeedlessProxy() {
        return false;
    }

    @Override
    public Class<? extends ProxyRequest> requestType() {
        return ProxyRequest.class;
    }

    private void doConnect(boolean sync) {
        if(active)
            throw new IllegalStateException("This component has been connect.");

        String host = config.getHost();
        int port = config.getPort();

        if(log.isInfoEnabled())
            log.info("Connect to flyingsocks server {}:{}...", host, port);

        Bootstrap b = bootstrap.clone();
        ChannelFuture f = b.connect(host, port);

        CountDownLatch waitLatch = new CountDownLatch(1);

        f.addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) {
                if (future.isSuccess()) {
                    if(log.isInfoEnabled())
                        log.info("connect success to flyingsocks server {}:{}", host, port);

                    active = true;
                    for(int i = 0; i < DEFAULT_PROCESSOR_THREAD; i++) {
                        clientMessageProcessor.submit(new ClientMessageTransferTask());
                    }
                    //连接成功后注册
                    getParentComponent().registerSubscriber(ProxyServerComponent.this);
                    f.removeListener(this);
                } else {
                    Throwable t = future.cause();
                    /*if(!(t instanceof IllegalStateException && t.getMessage().equals("executor not accepting a task")))
                        if (log.isWarnEnabled())
                            log.warn("can not connect to flyingsocks server, cause:", t);*/
                    log.warn("can not connect to flyingsocks server, cause:", t);
                    f.removeListener(this);
                    synchronized (ProxyServerComponent.this) {
                        if (!ProxyServerComponent.this.getState().after(LifecycleState.STOPING))
                            stop();
                    }
                }

                if(sync)
                    waitLatch.countDown();
            }
        });

        try {
            if (sync)
                waitLatch.await();
        } catch (InterruptedException e) {
            if(log.isWarnEnabled())
                log.warn("ProxyServerComponent interrupted when synchronize doConnect");
        }
    }

    @Override
    protected void stopInternal() {
        log.info("Ready to stop ProxyServerComponent {}:{}...", config.getHost(), config.getPort());
        active = false;
        getParentComponent().removeSubscriber(this);
        getParentComponent().removeProxyServer(this);
        getParentComponent().getParentComponent().getConfigManager()
                .removeConfig(OpenSSLConfig.generalName(config.getHost()));
        if(loopGroup != null)
            loopGroup.shutdownGracefully().addListener(future -> {
                if (future.isSuccess())
                    active = false;
                else {
                    Throwable t = future.cause();
                    if (log.isWarnEnabled())
                        log.warn("Shutdown Component " + getName() + "Failure, cause:", t);
                }
            });

        clientMessageProcessor.shutdownNow();
        activeProxyRequestMap.clear();
        super.stopInternal();
        log.info("Stop ProxyServerComponent {}:{} complete.", config.getHost(), config.getPort());
    }

    /**
     * 当不采用这个flyingsocks服务器时调用
     */
    public synchronized void unused() {
        use = false;
        if(active) {
            active = false;
            getParentComponent().removeProxyServer(this);
            stop();
        }
    }

    /**
     * 判断该服务器连接是否活跃
     */
    public boolean isActive() {
        return active;
    }

    public boolean isUse() {
        return use;
    }

    private synchronized void afterChannelInactive() {
        if(log.isInfoEnabled())
            log.info("Disconnect with flyingsocks server {}:{}", config.getHost(), config.getPort());

        getParentComponent().removeSubscriber(this); //移除订阅，防止在此期间请求涌入队列
        active = false;
        //清空队列
        proxyRequestQueue.clear();
        clientMessageProcessor.shutdownNow();
        loopGroup.shutdownGracefully();

        activeProxyRequestMap.clear();

        if(use) {
            if(log.isInfoEnabled())
                log.info("Retry to connect flyingsocks server {}:{}", config.getHost(), config.getPort());
            proxyServerSession = null;
            taskWaitLatch = new CountDownLatch(1);
            loopGroup = new NioEventLoopGroup(2);
            clientMessageProcessor = createMessageExecutor();
            doConnect(false);
        }
    }


    private final class InitialHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            if(log.isTraceEnabled())
                log.trace("Start flyingsocks server connection initialize");
            ProxyServerSession session = new ProxyServerSession((SocketChannel) ctx.channel());

            Random random = new Random();
            byte[] delimiter = new byte[DelimiterMessage.DEFAULT_SIZE];
            random.nextBytes(delimiter);

            DelimiterMessage msg = new DelimiterMessage(delimiter);

            session.setDelimiter(delimiter);
            ProxyServerComponent.this.proxyServerSession = session;

            try {
                ctx.writeAndFlush(msg.serialize());
                ctx.fireChannelActive();
            } catch (SerializationException e) {
                log.error("Serialize DelimiterMessage occur a exception", e);
                ctx.close();
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object buf) {
            if(buf instanceof ByteBuf) {
                if(log.isTraceEnabled())
                    log.trace("Receiver flyingsocks delimiter message");
                ByteBuf msg = (ByteBuf) buf;
                try {
                    DelimiterMessage dmsg = new DelimiterMessage(msg);
                    byte[] b = new byte[DelimiterMessage.DEFAULT_SIZE];
                    ByteBuf dbuf = dmsg.getDelimiter();
                    dbuf.copy().readBytes(b);

                    byte[] rb = proxyServerSession.getDelimiter();

                    for(int i = 0; i < DelimiterMessage.DEFAULT_SIZE; i++) {
                        if(b[i] != rb[i]) {
                            if(log.isDebugEnabled())
                                log.debug("Channel close because of delimiter is difference");
                            ctx.close();
                            return;
                        }
                    }

                    ctx.pipeline().remove(this)
                            .addLast(new DelimiterBasedFrameDecoder(102400, dbuf.copy()))
                            .addLast(new DelimiterOutboundHandler())
                            .addLast(new AuthHandler());

                } catch (SerializationException e) {
                    if(log.isWarnEnabled())
                        log.warn("DelimiterMessage serialization error", e);
                    ctx.close();
                }
            } else {
                ctx.fireChannelRead(buf);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(log.isWarnEnabled())
                log.warn("ProxyServerComponent occur a error" , cause);
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            afterChannelInactive();
        }
    }


    private final class DelimiterOutboundHandler extends ChannelOutboundHandlerAdapter {

        private final byte[] delimiter;

        DelimiterOutboundHandler() {
            this.delimiter = proxyServerSession.getDelimiter();
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            if(msg instanceof ByteBuf) {
                VoidChannelPromise vcp = new VoidChannelPromise(ctx.channel(), true);
                ByteBuf delimiter = Unpooled.buffer(this.delimiter.length).writeBytes(this.delimiter);
                ctx.write(msg, vcp);
                ctx.write(delimiter, vcp);
            } else {
                ctx.write(msg, promise);
            }
        }
    }


    private final class AuthHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            AuthMessage msg;
            switch (config.getAuthType()) {
                case SIMPLE: msg = new AuthMessage(AuthMessage.AuthMethod.SIMPLE); break;
                case USER: msg = new AuthMessage(AuthMessage.AuthMethod.USER); break;
                default:
                    throw new IllegalArgumentException("Auth method: " + config.getAuthType() + " Not support.");
            }

            List<String> keys = msg.getAuthMethod().getContainsKey();
            for(String key : keys) {
                msg.putContent(key, config.getAuthArgument(key));
            }
            try {
                ctx.writeAndFlush(msg.serialize());
                ctx.pipeline().remove(this).addLast(new ProxyHandler());
            } catch (SerializationException e) {
                log.error("Serialize AuthMessage occur a exception:", e);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(!ctx.channel().isActive()) {
                log.warn(String.format("[%s:%d]AuthHandler occur a exception", config.getHost(), config.getPort()), cause);
                afterChannelInactive();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if(log.isTraceEnabled())
                log.trace("Auth failure, from server {}:{}", config.getHost(), config.getPort());
            afterChannelInactive();
        }
    }


    private final class ProxyHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            taskWaitLatch.countDown();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if(log.isWarnEnabled())
                log.warn(String.format("Flyingsocks server connection %s:%d occur a exception",
                        config.getHost(), config.getPort()), cause);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if(msg instanceof ByteBuf) {
                ProxyResponseMessage resp;
                try {
                    resp = new ProxyResponseMessage((ByteBuf) msg);
                } catch (SerializationException e) {
                    if(log.isWarnEnabled()) {
                        log.warn("Serialize ProxyResponseMessage error", e);
                    }
                    ctx.close();
                    return;
                }

                if(resp.getState() == ProxyResponseMessage.State.SUCCESS) {
                    ProxyRequest req = activeProxyRequestMap.get(resp.getChannelId());
                    if(req == null)
                        return;
                    Channel cc;
                    if((cc = req.getClientChannel()).isActive()) {
                        cc.writeAndFlush(resp.getMessage());
                    }
                }

            } else {
                ctx.fireChannelRead(msg);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            afterChannelInactive();
        }
    }

    @Override
    public void receive(ProxyRequest request) {
        proxyRequestQueue.add(request);
        activeProxyRequestMap.put(request.getClientChannel().id().asShortText(), request);
    }


    private final class ClientMessageTransferTask implements Runnable {
        private final List<ProxyRequest> requests = new LinkedList<>();

        @Override
        public void run() {
            try {
                taskWaitLatch.await();
            } catch (InterruptedException e) {
                return;
            }

            if(log.isTraceEnabled())
                log.trace("Server:{} ClientMessageTransferTask start", config.getHost());

            Thread t = Thread.currentThread();
            begin:
            while(!t.isInterrupted()) {
                try {
                    ProxyRequest pr;
                    try {
                        while ((pr = proxyRequestQueue.poll(1, TimeUnit.MILLISECONDS)) != null) {
                            if(pr.sureMessageOnlyOne()) {
                                sendToProxyServer(pr, pr.getClientMessage());
                                continue begin;
                            }
                            requests.add(pr);
                        }
                    } catch (InterruptedException e) {
                        break;
                    }

                    ListIterator<ProxyRequest> it = requests.listIterator();
                    while (it.hasNext()) {
                        ProxyRequest req = it.next();
                        Channel cc = req.getClientChannel();
                        if (!cc.isActive()) {
                            it.remove();
                            activeProxyRequestMap.remove(req.getClientChannel().id().asShortText());
                            continue;
                        }

                        boolean isWrite = false;
                        CompositeByteBuf buf = Unpooled.compositeBuffer();
                        ByteBuf b;
                        while ((b = req.getClientMessage()) != null) {
                            buf.addComponent(true, b);
                            isWrite = true;
                        }

                        if (isWrite) {
                            sendToProxyServer(req, buf);
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception cause at ClientMessageTransferTask", e);
                }
            }

            for(ProxyRequest request : requests) {
                request.getClientChannel().close();
            }

        }

        private void sendToProxyServer(ProxyRequest request, ByteBuf buf) {
            if(proxyServerSession == null) {
                ReferenceCountUtil.release(buf);
                return;
            }
            ProxyRequestMessage prm = new ProxyRequestMessage(request.getClientChannel().id().asShortText());
            prm.setHost(request.getHost());
            prm.setPort(request.getPort());
            prm.setMessage(buf);

            try {
                proxyServerSession.socketChannel().writeAndFlush(prm.serialize());
            } catch (SerializationException e) {
                if(log.isWarnEnabled())
                    log.warn("Serialize ProxyRequestMessage occur a exception");
                request.getClientChannel().close();
            }
        }
    }

    private final class ConfigRemovedListener implements ConfigEventListener {
        @Override
        public void configEvent(ConfigEvent event) {
            if(event.getEvent().equals(Config.UPDATE_EVENT) && event.getSource() instanceof ProxyServerConfig) {
                ProxyServerConfig psc = (ProxyServerConfig) event.getSource();
                if(!psc.containsProxyServerNode(config)) {
                    synchronized (ProxyServerComponent.this) {
                        if(!ProxyServerComponent.this.getState().after(LifecycleState.STOPING)) {
                            stop();
                            parent.removeComponentByName(getName());
                            event.getConfigManager().removeConfigEventListener(this);
                        }
                    }
                }
            }
        }
    }

    public static String generalName(String host, int port) {
        return String.format("ProxyServerComponent-%s:%d", host, port);
    }

    private static ExecutorService createMessageExecutor() {
        return new ThreadPoolExecutor(DEFAULT_PROCESSOR_THREAD, DEFAULT_PROCESSOR_THREAD, 0, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>());
    }
}
