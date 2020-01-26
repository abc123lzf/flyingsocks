package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.GlobalConfig;
import com.lzf.flyingsocks.encrypt.EncryptProvider;
import com.lzf.flyingsocks.encrypt.EncryptSupport;
import com.lzf.flyingsocks.encrypt.OpenSSLEncryptProvider;
import com.lzf.flyingsocks.protocol.*;

import com.lzf.flyingsocks.util.FSMessageChannelOutboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static com.lzf.flyingsocks.protocol.AuthMessage.AuthMethod;
import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.EncryptType;

/**
 * flyingsocks服务器的连接管理组件，每个ProxyServerComponent对象代表一个服务器节点
 * 例如：若需要连接多个flyingsocks服务器实现负载均衡，则需要多个ProxyServerComponent对象
 *
 */
public class ProxyServerComponent extends AbstractComponent<ProxyComponent> implements ProxyRequestSubscriber {

    /**
     * 当前ProxyServerComponent负责传输客户端数据的最大线程数量
     */
    private static final int DEFAULT_PROCESSOR_THREAD;

    static {
        int cpus = Runtime.getRuntime().availableProcessors();
        if(cpus >= 12) {  //最多4个线程
            DEFAULT_PROCESSOR_THREAD = 4;
        } else if(cpus >= 4) {
            DEFAULT_PROCESSOR_THREAD = 2;
        } else {
            DEFAULT_PROCESSOR_THREAD = 1;
        }
    }

    //该服务器节点配置信息
    private final ProxyServerConfig.Node config;

    //客户端是否启动了这个节点
    private volatile boolean use;

    //该节点连接状态是否活跃
    private volatile boolean active = false;

    //连接状态
    private volatile ConnectionState connectionState = ConnectionState.NEW;

    //连接线程池
    private volatile EventLoopGroup loopGroup;

    //通用Netty引导对象
    private volatile Bootstrap bootstrap;

    //用于处理客户端消息
    private volatile ExecutorService clientMessageProcessor;

    //与flyingsocks服务器会话对象
    private volatile ProxyServerSession proxyServerSession;

    //下次重连的时间(绝对时间戳)
    private volatile long nextReconnectTime = -1L;

    //代理请求消息队列
    private final BlockingQueue<SerialProxyRequest> proxyRequestQueue = new LinkedBlockingQueue<>();

    //活跃的代理请求Map
    private final Map<Integer, SerialProxyRequest> activeProxyRequestMap = new ConcurrentHashMap<>(512);

    //代理请求ID生成器
    private final AtomicInteger serialBuilder = new AtomicInteger(0);

    //确保可以正式向服务器发送代理请求后释放clientMessageProcessor中的等待线程
    private volatile CountDownLatch taskWaitLatch;

    /**
     * @param proxyComponent 父组件引用
     * @param config 该FS服务器的配置对象
     */
    ProxyServerComponent(ProxyComponent proxyComponent, ProxyServerConfig.Node config) {
        super(generalName(config.getHost(), config.getPort()), Objects.requireNonNull(proxyComponent));
        this.config = Objects.requireNonNull(config);
        this.use = config.isUse();
    }


    private static final class SerialProxyRequest extends ProxyRequest {

        private final int serialId;

        private final ProxyRequest request;

        SerialProxyRequest(int serialId, ProxyRequest request) {
            super(request.host, request.port, request.clientChannel, request.protocol);
            this.serialId = serialId;
            this.request = request;
        }

        @Override
        public ByteBuf takeClientMessage() {
            return request.takeClientMessage();
        }

        @Override
        public boolean ensureMessageOnlyOne() {
            return request.ensureMessageOnlyOne();
        }
    }


    /**
     * 代理服务器连接初始化逻辑，执行步骤为:
     * 1.获取与服务器的加密方式，如果需要SSL加密，则建立一个证书连接接收服务器证书
     * 2.收取证书后，与服务器建立正式的代理连接
     */
    @Override
    protected void initInternal() {
        ConfigManager<?> cm = parent.getParentComponent().getConfigManager();
        GlobalConfig cfg = cm.getConfig(GlobalConfig.NAME, GlobalConfig.class);

        EncryptProvider provider;
        //目前仅支持OpenSSL加密和不加密(测试性质)
        if(config.getEncryptType() == EncryptType.NONE) {
            provider = null;
        } else if(config.getEncryptType() == EncryptType.SSL) {
            updateConnectionState(ConnectionState.SSL_INITIAL);
            //首先计算证书文件的MD5
            final byte[] md5 = calcuateCertFileMD5(cfg.configPath());
            if(md5 == null) {
                throw new ComponentException();
            }

            final Thread thread = Thread.currentThread();
            final String host = config.getHost();
            final int certPort = config.getCertPort();
            //尝试从服务器上获取SSL证书
            final EventLoopGroup sslGroup = new NioEventLoopGroup(1);
            Bootstrap sslBoot = new Bootstrap()
                    .group(sslGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, cfg.getConnectTimeout())
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline cp = ch.pipeline();
                            cp.addLast(FSMessageChannelOutboundHandler.INSTANCE);
                            cp.addLast(new DelimiterBasedFrameDecoder(1024 * 100,
                                    Unpooled.copiedBuffer(CertResponseMessage.END_MARK)));

                            cp.addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    updateConnectionState(ConnectionState.SSL_CONNECT);
                                    CertRequestMessage msg;
                                    switch (config.getAuthType()) {
                                        case SIMPLE: msg = new CertRequestMessage(AuthMethod.SIMPLE, md5); break;
                                        case USER: msg = new CertRequestMessage(AuthMethod.USER, md5); break;
                                        default:
                                            throw new IllegalArgumentException("Auth method: " + config.getAuthType() + " Not support.");
                                    }

                                    List<String> keys = msg.getAuthMethod().getContainsKey();
                                    for(String key : keys) {
                                        msg.putContent(key, config.getAuthArgument(key));
                                    }

                                    log.trace("Ready to send CertRequest to remote server {}:{}", host, certPort);

                                    ctx.writeAndFlush(msg);
                                }
                            });

                            cp.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                private boolean success = false;
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
                                    CertResponseMessage msg = new CertResponseMessage(buf);
                                    if(msg.needUpdate()) {
                                        InputStream is = msg.getFile();
                                        int len = msg.getLength();
                                        writeCertFile(cfg.configPath(), is, len);
                                        if(log.isInfoEnabled())
                                            log.info("Update cert file from remote flyingsocks server {}:{}", host, certPort);

                                    } else if(log.isTraceEnabled()) {
                                        log.trace("Remote server cert file is same as local, from {}:{}", host, certPort);
                                    }

                                    success = true;
                                    ctx.close();
                                }

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    if(success) {
                                        updateConnectionState(ConnectionState.SSL_CONNECT_DONE);
                                    } else {
                                        updateConnectionState(ConnectionState.SSL_CONNECT_AUTH_FAILURE);
                                    }

                                    LockSupport.unpark(thread);
                                    super.channelInactive(ctx);
                                }
                            });
                        }
                    });

            updateConnectionState(ConnectionState.SSL_CONNECTING);
            //连接到服务器的证书端口
            sslBoot.connect(host, certPort).addListener(f -> {
                if(!f.isSuccess()) {
                    Throwable t = f.cause();
                    if(t instanceof ConnectTimeoutException) {
                        updateConnectionState(ConnectionState.SSL_CONNECT_TIMEOUT);
                    } else {
                        updateConnectionState(ConnectionState.SSL_CONNECT_ERROR);
                    }
                    LockSupport.unpark(thread);
                }
            });

            //等待上述证书操作的完成
            LockSupport.parkNanos((cfg.getConnectTimeout() + 1000 * 20) * 1_000_000L);  //20秒加上连接超时时间
            sslGroup.shutdownGracefully();

            if(!connectionState.isNormal()) {
                log.warn("Can not connect to cert service {}:{}", host, config.getCertPort());
                stop();
                return;
            }

            OpenSSLConfig sslcfg = new OpenSSLConfig(cm, host, config.getPort());
            cm.registerConfig(sslcfg);

            provider = EncryptSupport.lookupProvider("OpenSSL", OpenSSLEncryptProvider.class);
            Map<String, Object> params = new HashMap<>();
            params.put("client", true);
            try {
                params.put("file.cert.root", sslcfg.openRootCertStream());
            } catch (IOException e) {
                log.error("Read CA cert file occur a exception, from {}:{}", host, config.getPort(), e);
                stop();
                return;
            }

            try {
                provider.initialize(params);
            } catch (Exception e) {
                throw new ComponentException(e);
            }
        } else {
            throw new ComponentException("Unsupport encrypt type " + config.getEncryptType());
        }

        taskWaitLatch = new CountDownLatch(1);

        Map<String, Object> params = new HashMap<>(2);
        params.put("alloc", PooledByteBufAllocator.DEFAULT);

        loopGroup = new NioEventLoopGroup(1);
        bootstrap = new Bootstrap()
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, cfg.getConnectTimeout())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline cp = ch.pipeline();
                        if(provider != null) {
                            if(!provider.isInboundHandlerSameAsOutboundHandler()) {
                                cp.addLast(provider.encodeHandler(params));
                            }
                            cp.addLast(provider.decodeHandler(params));
                        }
                        cp.addLast(FSMessageChannelOutboundHandler.INSTANCE);
                        cp.addLast(new InitialHandler());
                    }
                });

        clientMessageProcessor = createMessageExecutor();

        super.initInternal();
    }

    @Override
    protected void startInternal() {
        if(!connectionState.isNormal() || connectionState == ConnectionState.UNUSED) {
            return;
        }

        if(isUse()) {
            doConnect(true);
        }

        ConfigManager<?> cm = parent.getParentComponent().getConfigManager();
        cm.registerConfigEventListener(new ConfigRemovedListener());

        super.startInternal();

        if(isActive()) {
            parent.addActiveProxyServer(this);
        }
    }


    /**
     * 更新连接状态
     * @param state 状态
     */
    private void updateConnectionState(ConnectionState state) {
        this.connectionState = state;
    }

    /**
     * @return 获取连接状态
     */
    public ConnectionState connectionState() {
        return connectionState;
    }

    /**
     * @return 只接收需要代理的请求
     */
    @Override
    public boolean receiveNeedProxy() {
        return true;
    }

    /**
     * @return 不接收无需代理的请求
     */
    @Override
    public boolean receiveNeedlessProxy() {
        return false;
    }

    /**
     * @return 可以处理所有类型的ProxyRequest
     */
    @Override
    public Class<? extends ProxyRequest> requestType() {
        return ProxyRequest.class;
    }


    private void doConnect(boolean sync) {
        if(active)
            throw new IllegalStateException("This component has been connect.");

        updateConnectionState(ConnectionState.PROXY_INITIAL);
        String host = config.getHost();
        int port = config.getPort();

        if(log.isInfoEnabled())
            log.info("Connect to flyingsocks server {}:{}...", host, port);

        Bootstrap b = bootstrap.clone().group(this.loopGroup);
        updateConnectionState(ConnectionState.PROXY_CONNECTING);

        ChannelFuture f = b.connect(host, port);
        final CountDownLatch waitLatch = new CountDownLatch(1);

        f.addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) {
                if (future.isSuccess()) {
                    log.info("Connect success to flyingsocks server {}:{}", host, port);

                    active = true;
                    for(int i = 0; i < DEFAULT_PROCESSOR_THREAD; i++) {
                        clientMessageProcessor.submit(new ClientMessageTransferTask());
                    }

                    nextReconnectTime = -1L;
                    //连接成功后注册
                    parent.registerSubscriber(ProxyServerComponent.this);
                    f.removeListener(this);
                } else {
                    Throwable t = future.cause();
                    if(log.isWarnEnabled()) {
                        log.warn("Can not connect to flyingsocks server, cause:", t);
                    }

                    if(t instanceof ConnectTimeoutException) {
                        updateConnectionState(ConnectionState.PROXY_CONNECT_TIMEOUT);
                    } else {
                        updateConnectionState(ConnectionState.PROXY_CONNECT_ERROR);
                    }

                    f.removeListener(this);

                    if(connectionState.canRetry()) {
                        afterChannelInactive(); //重新尝试连接
                    }
                }

                if(sync) {
                    waitLatch.countDown();
                }
            }
        });

        if(sync) {
            try {
                waitLatch.await(10000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                if (log.isWarnEnabled())
                    log.warn("ProxyServerComponent interrupted when synchronize doConnect");
            }
        }
    }


    @Override
    protected void stopInternal() {
        if(log.isInfoEnabled()) {
            log.info("Ready to stop ProxyServerComponent {}:{}...", config.getHost(), config.getPort());
        }

        updateConnectionState(ConnectionState.UNUSED);
        active = false;
        parent.removeSubscriber(this);
        parent.removeProxyServer(this);
        parent.getParentComponent().getConfigManager().removeConfig(OpenSSLConfig.generalName(config.getHost(), config.getPort()));

        if(loopGroup != null) {
            loopGroup.shutdownGracefully().addListener(future -> {
                if (future.isSuccess())
                    active = false;
                else {
                    Throwable t = future.cause();
                    if (log.isWarnEnabled())
                        log.warn("Shutdown Component " + getName() + "Failure, cause:", t);
                }
            });
        }

        if(clientMessageProcessor != null) {
            clientMessageProcessor.shutdownNow();
        }

        activeProxyRequestMap.clear();
        super.stopInternal();
        if(log.isInfoEnabled()) {
            log.info("Stop ProxyServerComponent {}:{} complete.", config.getHost(), config.getPort());
        }
    }

    /**
     * @return 判断该服务器连接是否活跃
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @return 该服务器是否处于启用状态
     */
    public boolean isUse() {
        return use;
    }


    void setUse(boolean use) {
        this.use = use;
    }

    /**
     * 连接失效后的处理逻辑
     */
    private synchronized void afterChannelInactive() {
        if(log.isInfoEnabled())
            log.info("Disconnect with flyingsocks server {}:{}", config.getHost(), config.getPort());

        parent.removeSubscriber(this); //移除订阅，防止在此期间请求涌入队列
        active = false;
        //清空队列
        proxyRequestQueue.clear();
        clientMessageProcessor.shutdownNow();
        loopGroup.shutdownGracefully();

        activeProxyRequestMap.clear();

        if(!connectionState.isNormal() && !connectionState.canRetry()) {
            return;
        }

        //处理掉线重连
        //如果父组件没有处于正在停止状态并且用户还希望继续使用该节点
        if(!parent.getState().after(LifecycleState.STOPING) && use) {
            if (nextReconnectTime == -1L) {
                nextReconnectTime = 2 * 1000;
            } else {
                nextReconnectTime = nextReconnectTime >= 30000 ? 30000 : nextReconnectTime * 2;
            }
            long d = nextReconnectTime;
            if(d > 2000L) {
                if(log.isInfoEnabled())
                    log.info("Waiting {}ms before reconnect server {}:{}", d, config.getHost(), config.getPort());
                Thread.interrupted(); //上述clientMessageProcessor和loopGroup调用了
                try {
                    wait(d);
                } catch (InterruptedException ignore) {
                }
                //wait会释放锁所以再一次检查
                if(parent.getState().after(LifecycleState.STOPING) || !use) {
                    return;
                }
            }

            if(log.isInfoEnabled())
                log.info("Retry to connect flyingsocks server {}:{}", config.getHost(), config.getPort());
            this.proxyServerSession = null;
            this.taskWaitLatch = new CountDownLatch(1);
            this.loopGroup = new NioEventLoopGroup(2);
            this.clientMessageProcessor = createMessageExecutor();
            doConnect(false);
        }
    }

    /**
     * 计算证书文件MD5值
     * @param location 存放证书文件的路径
     * @return MD5值，若文件不存在则返回一个长度为16，值全部为0的byte数组
     */
    private byte[] calcuateCertFileMD5(String location) {
        File folder = new File(location, OpenSSLConfig.folderName(config.getHost(), config.getPort()));

        if(!folder.exists()) {
            if(!folder.mkdirs()) {
                log.error("Can not create folder at {}", folder.getAbsolutePath());
                return null;
            }
            return new byte[16];
        } else if(folder.isFile()) {
            if(!folder.delete()) {
                log.error("Location {} exists a file and can not delete.", folder.getName());
                return null;
            }
        }

        if(folder.isDirectory()) {
            File file = new File(folder, OpenSSLConfig.CERT_FILE_NAME);
            if(!file.exists() || file.length() == 0L) {
                return new byte[16];
            }

            if(file.isDirectory() && !file.delete()) {
                log.error("location {} exists a folder and can not delete.", file.getAbsolutePath());
                return null;
            }

            try(FileInputStream fis = new FileInputStream(file)) {
                byte[] b = new byte[10240];
                int len = fis.read(b);
                byte[] rb = new byte[len];
                System.arraycopy(b, 0, rb, 0, len);
                MessageDigest md = MessageDigest.getInstance("MD5");
                return md.digest(rb);
            } catch (IOException e) {
                log.error("Read file ca.crt occur a exception", e);
                return null;
            } catch (NoSuchAlgorithmException e) {
                throw new Error(e);
            }
        }

        return null;
    }


    private void writeCertFile(String location, final InputStream is, final int len) throws IOException {
        File file = new File(new File(location, OpenSSLConfig.folderName(config.getHost(), config.getPort())), OpenSSLConfig.CERT_FILE_NAME);
        byte[] b = new byte[len];
        int r = is.read(b);
        if(r != len) {
            throw new IOException("File real size is different from server message");
        }

        try(FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(b);
            fos.flush();
        }
    }


    private final class InitialHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.trace("Start flyingsocks server connection initialize");
            updateConnectionState(ConnectionState.PROXY_CONNECT);
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
                log.trace("Receive flyingsocks delimiter message");
                ByteBuf msg = (ByteBuf) buf;
                try {
                    DelimiterMessage dmsg = new DelimiterMessage(msg);
                    byte[] b = new byte[DelimiterMessage.DEFAULT_SIZE];
                    ByteBuf dbuf = dmsg.getDelimiter();
                    dbuf.copy().readBytes(b);

                    byte[] rb = proxyServerSession.getDelimiter();

                    if(rb == null) {
                        log.info("Delimiter message magic number is not correct");
                        ctx.close();
                        return;
                    }

                    for(int i = 0; i < DelimiterMessage.DEFAULT_SIZE; i++) {
                        if(b[i] != rb[i]) {
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
                    log.warn("DelimiterMessage serialization error", e);
                    ctx.close();
                } finally {
                    ReferenceCountUtil.release(msg);
                }
            } else {
                ctx.fireChannelRead(buf);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            updateConnectionState(ConnectionState.PROXY_CONNECT_ERROR);
            log.warn("ProxyServerComponent occur a error" , cause);
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            updateConnectionState(ConnectionState.PROXY_DISCONNECT);  //在初始化过程中有可能因为网络波动断开连接
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
                ChannelPromise vcp = ctx.voidPromise();
                ByteBuf delimiter = PooledByteBufAllocator.DEFAULT.buffer(this.delimiter.length).writeBytes(this.delimiter);
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
                case SIMPLE: msg = new AuthMessage(AuthMethod.SIMPLE); break;
                case USER: msg = new AuthMessage(AuthMethod.USER); break;
                default:
                    throw new IllegalArgumentException("Auth method: " + config.getAuthType() + " Not support.");
            }

            List<String> keys = msg.getAuthMethod().getContainsKey();
            for(String key : keys) {
                msg.putContent(key, config.getAuthArgument(key));
            }

            ctx.writeAndFlush(msg);
            ctx.pipeline().remove(this).addLast(new ProxyHandler());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(!ctx.channel().isActive()) {
                if(log.isWarnEnabled())
                    log.warn(String.format("[%s:%d]AuthHandler occur a exception", config.getHost(), config.getPort()), cause);
                updateConnectionState(ConnectionState.PROXY_CONNECT_ERROR);
                afterChannelInactive();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if(log.isTraceEnabled())
                log.trace("Auth failure, from server {}:{}", config.getHost(), config.getPort());
            updateConnectionState(ConnectionState.PROXY_CONNECT_AUTH_FAILURE);  //多半是认证失败
            afterChannelInactive();
        }
    }


    private final class ProxyHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            taskWaitLatch.countDown();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(cause instanceof IOException && log.isInfoEnabled()) {
                updateConnectionState(ConnectionState.PROXY_DISCONNECT);
                log.info("flyingsocks server {}:{} force closure of connections", config.getHost(), config.getPort());
            } else if(log.isWarnEnabled()) {
                updateConnectionState(ConnectionState.PROXY_CONNECT_ERROR);
                log.warn(String.format("flyingsocks server connection %s:%d occur a exception",
                        config.getHost(), config.getPort()), cause);
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if(msg instanceof ByteBuf) {
                try {
                    ProxyResponseMessage resp;
                    try {
                        resp = new ProxyResponseMessage((ByteBuf) msg);
                    } catch (SerializationException e) {
                        if (log.isWarnEnabled())
                            log.warn("Serialize ProxyResponseMessage error", e);
                        ctx.close();
                        return;
                    }

                    if (resp.getState() == ProxyResponseMessage.State.SUCCESS) {
                        ProxyRequest req = activeProxyRequestMap.get(resp.serialId());
                        if (req == null) {
                            return;
                        }
                        Channel cc;
                        if ((cc = req.clientChannel()).isActive())
                            cc.writeAndFlush(resp.getMessage());
                    }
                } finally {
                    ReferenceCountUtil.release(msg);
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
        final int id = serialBuilder.getAndIncrement();
        final SerialProxyRequest req = new SerialProxyRequest(id, request);
        proxyRequestQueue.add(req);
        activeProxyRequestMap.put(id, req);
    }


    private final class ClientMessageTransferTask implements Runnable {
        private final List<SerialProxyRequest> requests = new LinkedList<>();


        @Override
        public void run() {
            try {
                taskWaitLatch.await();
            } catch (InterruptedException e) {
                return;
            }

            if(log.isTraceEnabled())
                log.trace("Server: {}:{} ClientMessageTransferTask start", config.getHost(), config.getPort());

            Thread t = Thread.currentThread();
            begin:
            while(!t.isInterrupted()) {
                try {
                    SerialProxyRequest pr;
                    try {
                        while ((pr = proxyRequestQueue.poll(1, TimeUnit.MILLISECONDS)) != null) {
                            if(pr.ensureMessageOnlyOne()) {
                                ByteBuf buf = pr.takeClientMessage();
                                sendToProxyServer(pr, buf);
                                ReferenceCountUtil.release(buf);
                                continue begin;
                            }
                            requests.add(pr);
                        }
                    } catch (InterruptedException e) {
                        break;
                    }

                    Iterator<SerialProxyRequest> it = requests.iterator();
                    while (it.hasNext()) {
                        SerialProxyRequest req = it.next();
                        Channel cc = req.clientChannel();
                        if (!cc.isActive() || req.isCtlMark(31)) {
                            it.remove();
                            activeProxyRequestMap.remove(req.serialId);
                        } else {
                            boolean isWrite = false;
                            CompositeByteBuf buf = PooledByteBufAllocator.DEFAULT.compositeBuffer();
                            ByteBuf b;
                            while ((b = req.takeClientMessage()) != null) {
                                buf.addComponent(true, b);
                                isWrite = true;
                            }

                            if (isWrite) {
                                sendToProxyServer(req, buf);
                            }
                        }
                    }

                } catch (Exception e) {
                    log.error("Exception cause at ClientMessageTransferTask", e);
                }
            }

            for(SerialProxyRequest request : requests) {
                request.clientChannel().close();
            }

        }

        private void sendToProxyServer(SerialProxyRequest request, ByteBuf buf) {
            if(proxyServerSession == null) {
                buf.release();
                return;
            }

            ProxyRequestMessage prm = new ProxyRequestMessage(request.serialId,
                    request.protocol.toMessageType());

            prm.setHost(request.getHost());
            prm.setPort(request.getPort());
            prm.setMessage(buf);

            try {
                proxyServerSession.socketChannel().writeAndFlush(prm.serialize());
            } catch (SerializationException e) {
                if(log.isWarnEnabled())
                    log.warn("Serialize ProxyRequestMessage occur a exception");
                request.clientChannel().close();
            } finally {
                buf.release();
            }
        }
    }

    /**
     * 负责监听配置被移除时(用户删除正在使用的FS服务器)停止当前组件
     */
    private final class ConfigRemovedListener implements ConfigEventListener {
        @Override
        public void configEvent(ConfigEvent event) {
            if(event.getEvent().equals(Config.UPDATE_EVENT) && event.getSource() instanceof ProxyServerConfig) {
                ProxyServerConfig psc = (ProxyServerConfig) event.getSource();
                if(!psc.containsProxyServerNode(config)) {
                    synchronized (ProxyServerComponent.this) {
                        if(!getState().after(LifecycleState.STOPING)) {
                            stop();
                            assert parent != null;
                            parent.removeComponentByName(getName());
                            event.getConfigManager().removeConfigEventListener(this);
                        }
                    }
                }
            }
        }
    }

    static String generalName(String host, int port) {
        return String.format("ProxyServerComponent-%s:%d", host, port);
    }

    private static ExecutorService createMessageExecutor() {
        return new ThreadPoolExecutor(DEFAULT_PROCESSOR_THREAD, DEFAULT_PROCESSOR_THREAD + 2, 2, TimeUnit.MINUTES,
                new SynchronousQueue<>());
    }
}
