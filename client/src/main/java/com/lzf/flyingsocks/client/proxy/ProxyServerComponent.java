package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.protocol.*;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.*;

/**
 * flyingsocks服务器的连接管理组件，每个ProxyServerComponent对象代表一个服务器节点
 * 例如：若需要连接多个flyingsocks服务器实现负载均衡，则需要多个ProxyServerComponent对象
 *
 */
public class ProxyServerComponent extends AbstractComponent<ProxyComponent> {
    private static final int DEFAULT_PROCESSOR_THREAD = 4;

    //该服务器节点配置信息
    private final ProxyServerConfig.Node config;

    //客户端是否启动了这个节点
    private volatile boolean use;

    //该节点连接状态是否活跃
    private volatile boolean active = false;

    //连接线程池
    private EventLoopGroup loopGroup;

    //通用Netty引导对象
    private Bootstrap bootstrap;

    //SSL加密上下文对象
    private SslContext sslContext;

    //用于处理客户端消息
    private ExecutorService clientMessageProcessor;

    //与flyingsocks服务器会话对象
    private volatile ProxyServerSession proxyServerSession;

    //活跃的代理请求Map
    private final Map<String, ProxyRequest> activeProxyRequestMap = new ConcurrentHashMap<>(512);

    //确保可以正式向服务器发送代理请求后释放clientMessageProcessor中的等待线程
    private volatile CountDownLatch taskWaitLatch;

    public ProxyServerComponent(ProxyComponent proxyComponent, ProxyServerConfig.Node config) {
        super(generalName(config.getHost(), config.getPort()), proxyComponent);
        this.config = config;
        this.use = config.isUse();
    }

    @Override
    protected void initInternal() {
        try(InputStream is = getParentComponent().getParentComponent().loadResource(config.getJksPath())) {
            KeyStore key = KeyStore.getInstance("JKS");
            key.load(is, config.getJksPass().toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(key);
            sslContext = SslContextBuilder.forClient().trustManager(tmf).build();
        } catch (Exception e) {
            throw new ComponentException(e);
        }

        taskWaitLatch = new CountDownLatch(1);

        loopGroup = new NioEventLoopGroup(2);
        bootstrap = new Bootstrap().group(loopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline cp = ch.pipeline();
                        cp.addLast(new SslHandler(sslContext.newEngine(ch.alloc())));
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
            if(!active) {
                stop();
                return;
            }
        }

        super.startInternal();

        if(active)
            getParentComponent().addActiveProxyServer(this);
    }

    private void doConnect(boolean sync) {
        if(active)
            throw new IllegalStateException("This component has been connect.");

        String host = config.getHost();
        int port = config.getPort();

        if(log.isTraceEnabled())
            log.trace("Connect to flyingsocks server {}:{}...", host, port);

        Bootstrap b = bootstrap.clone();
        ChannelFuture f = b.connect(host, port);

        CountDownLatch waitLatch = new CountDownLatch(1);

        f.addListener(future -> {
            if (future.isSuccess()) {
                if(log.isTraceEnabled())
                    log.trace("connect success to flyingsocks server {}:{}", host, port);

                active = true;
                for(int i = 0; i < DEFAULT_PROCESSOR_THREAD; i++) {
                    clientMessageProcessor.submit(new ClientMessageTransferTask());
                }

            } else {
                Throwable t = future.cause();
                if (log.isWarnEnabled())
                    log.warn("can not connect to flyingsocks server, cause:", t);
            }

            if(sync)
                waitLatch.countDown();
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
        active = false;
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
        log.warn("AfterChannelInactive");
        active = false;
        clientMessageProcessor.shutdownNow();
        loopGroup.shutdownGracefully();

        activeProxyRequestMap.clear();

        if(use) {
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
                    dmsg.getDelimiter().readBytes(b);

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
                            .addLast(new DelimiterOutboundHandler()).addLast(new AuthHandler());

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
                ByteBuf delimiter = Unpooled.buffer(this.delimiter.length).writeBytes(this.delimiter);
                ctx.write(msg, promise);
                ctx.write(delimiter, promise);
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
                        cc.writeAndFlush(req.getClientMessage());
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


    private final class ClientMessageTransferTask implements Runnable, ProxyRequestSubscriber {
        private final List<ProxyRequest> requests = new LinkedList<>();
        private final List<ProxyRequest> tmpList = new ArrayList<>(50);

        @Override
        public void receive(ProxyRequest request) {
            if(taskWaitLatch == null || taskWaitLatch.getCount() > 0)
                return;

            if(request.sureMessageOnlyOne()) {
                ByteBuf buf = request.getClientMessage();
                sendToProxyServer(request, buf);
            } else {
                synchronized (tmpList) {
                    tmpList.add(request);
                }
            }

            activeProxyRequestMap.put(request.getClientChannel().id().asLongText(), request);
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

        @Override
        public void run() {
            try {
                taskWaitLatch.await();
            } catch (InterruptedException e) {
                return;
            }
            Thread t = Thread.currentThread();
            while(!t.isInterrupted()) {
                ListIterator<ProxyRequest> it = requests.listIterator();
                while(it.hasNext()) {
                    ProxyRequest req = it.next();
                    Channel cc = req.getClientChannel();
                    if(!cc.isActive()) {
                        it.remove();
                        activeProxyRequestMap.remove(req.getClientChannel().id().asLongText());
                        continue;
                    }

                    boolean isWrite = false;
                    CompositeByteBuf buf = Unpooled.compositeBuffer();
                    ByteBuf b;
                    while((b = req.getClientMessage()) != null) {
                        buf.addComponent(b);
                        isWrite = true;
                    }

                    if(isWrite)
                        sendToProxyServer(req, buf);
                }

                synchronized (tmpList) {
                    requests.addAll(tmpList);
                    tmpList.clear();
                }
            }

            for(ProxyRequest request : requests) {
                request.getClientChannel().close();
            }
        }

        private void sendToProxyServer(ProxyRequest request, ByteBuf buf) {
            ProxyRequestMessage prm = new ProxyRequestMessage(request.getClientChannel().id().asLongText());
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

    public static String generalName(String host, int port) {
        return String.format("ProxyServerComponent-%s:%d", host, port);
    }

    private static ExecutorService createMessageExecutor() {
        return new ThreadPoolExecutor(DEFAULT_PROCESSOR_THREAD, DEFAULT_PROCESSOR_THREAD, 0, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>());
    }
}
