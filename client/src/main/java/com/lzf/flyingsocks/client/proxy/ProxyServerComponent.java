package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.protocol.DelimiterMessage;
import com.lzf.flyingsocks.protocol.ProxyRequestMessage;
import com.lzf.flyingsocks.protocol.ProxyResponseMessage;
import com.lzf.flyingsocks.protocol.SerializationException;
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
public class ProxyServerComponent extends AbstractComponent<ProxyComponent<?>> {
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
    private ProxyServerSession proxyServerSession;

    //活跃的代理请求Map
    private final Map<String, ProxyRequest> activeProxyRequestMap = new ConcurrentHashMap<>(512);

    //确保可以正式向服务器发送代理请求后释放clientMessageProcessor中的等待线程
    private volatile CountDownLatch taskWaitLatch;

    public ProxyServerComponent(ProxyComponent<?> proxyComponent, ProxyServerConfig.Node config) {
        super(generalName(config.getHost(), config.getPort()), proxyComponent);
        this.config = config;
        this.use = config.isUse();
    }

    @Override
    protected void initInternal() {
        try {
            InputStream is = getParentComponent().getParentComponent().loadResource(config.getJksPath());
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

        clientMessageProcessor = new ThreadPoolExecutor(DEFAULT_PROCESSOR_THREAD, DEFAULT_PROCESSOR_THREAD, 0, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>());

        super.initInternal();
    }

    @Override
    protected void startInternal() {
        if(use) {
            doConnect(true);
        }

        super.startInternal();
    }

    @Override
    protected void restartInternal() {
        super.restartInternal();
    }

    @Override
    protected void stopInternal() {
        use = false;
        try {
            loopGroup.shutdownGracefully().addListener(future -> {
                if (future.isSuccess())
                    active = false;
                else {
                    Throwable t = future.cause();
                    if (log.isWarnEnabled())
                        log.warn("Shutdown Component " + getName() + "Failure, cause:", t);
                }
            }).sync();
        } catch (InterruptedException e) {
            //Should not be happend.
        }
        super.stopInternal();
    }

    public synchronized void activeConnection(boolean sync) {
        if(loopGroup.isShutdown())
            loopGroup = new NioEventLoopGroup(2);

        doConnect(sync);
    }

    public synchronized void disconnect() {
        if(active) {
            loopGroup.shutdownGracefully();
            active = false;
        }
    }

    private void doConnect(boolean sync) {
        if(active)
            throw new IllegalStateException("This component has been connect.");

        Bootstrap b = bootstrap.clone();
        ChannelFuture f = b.connect(config.getHost(), config.getPort()).addListener(future -> {
            if (future.isSuccess()) {
                active = true;
                for(int i = 0; i < DEFAULT_PROCESSOR_THREAD; i++) {
                    clientMessageProcessor.submit(new ClientMessageTransferTask());
                }

            } else {
                Throwable t = future.cause();
                if (log.isWarnEnabled())
                    log.warn("can not connect to flyingsocks server, cause:", t);
            }
        });

        try {
            if (sync)
                f.sync();
        } catch (InterruptedException e) {
            //Nothing to do.
        }
    }


    private final class InitialHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ProxyServerSession session = new ProxyServerSession((SocketChannel) ctx.channel());

            Random random = new Random();
            byte[] delimiter = new byte[DelimiterMessage.DEFAULT_SIZE];
            random.nextBytes(delimiter);

            DelimiterMessage msg = new DelimiterMessage(delimiter);

            session.setDelimiter(delimiter);
            ProxyServerComponent.this.proxyServerSession = session;

            ctx.writeAndFlush(msg);
            ctx.fireChannelActive();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object buf) {
            if(buf instanceof ByteBuf) {
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
                            .addLast(new DelimiterOutboundHandler()).addLast(new ProxyHandler());

                } catch (SerializationException e) {
                    ctx.close();
                }
            } else {
                ctx.fireChannelRead(buf);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if(log.isWarnEnabled())
                log.warn("ProxyServerComponent occur a error" , cause);
            ctx.close();
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


                ProxyRequest request;

                try {
                    while((request = getParentComponent().pollProxyRequest(true, 1, TimeUnit.MILLISECONDS)) != null) {
                        if(request.sureMessageOnlyOne()) {
                            ByteBuf buf = request.getClientMessage();
                            sendToProxyServer(request, buf);
                        } else {
                            requests.add(request);
                        }

                        activeProxyRequestMap.put(request.getClientChannel().id().asLongText(), request);
                    }
                } catch (InterruptedException e) {
                    break;
                }
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
}
