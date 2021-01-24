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
package com.lzf.flyingsocks.client.proxy.server;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.ConfigEvent;
import com.lzf.flyingsocks.ConfigEventListener;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.LifecycleState;
import com.lzf.flyingsocks.client.GlobalConfig;
import com.lzf.flyingsocks.client.proxy.ProxyComponent;
import com.lzf.flyingsocks.client.proxy.ProxyRequest;
import com.lzf.flyingsocks.client.proxy.ProxyRequestSubscriber;
import com.lzf.flyingsocks.client.proxy.util.MessageReceiver;
import com.lzf.flyingsocks.encrypt.EncryptProvider;
import com.lzf.flyingsocks.encrypt.EncryptSupport;
import com.lzf.flyingsocks.encrypt.OpenSSLEncryptProvider;
import com.lzf.flyingsocks.protocol.AuthMessage;
import com.lzf.flyingsocks.protocol.CertRequestMessage;
import com.lzf.flyingsocks.protocol.CertResponseMessage;
import com.lzf.flyingsocks.protocol.DelimiterMessage;
import com.lzf.flyingsocks.protocol.PingMessage;
import com.lzf.flyingsocks.protocol.PongMessage;
import com.lzf.flyingsocks.protocol.ProxyRequestMessage;
import com.lzf.flyingsocks.protocol.ProxyResponseMessage;
import com.lzf.flyingsocks.protocol.SerializationException;
import com.lzf.flyingsocks.util.DelimiterOutboundHandler;
import com.lzf.flyingsocks.util.FSMessageChannelOutboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static com.lzf.flyingsocks.client.proxy.server.ProxyServerConfig.EncryptType;
import static com.lzf.flyingsocks.protocol.AuthMessage.AuthMethod;

/**
 * flyingsocks服务器的连接管理组件，每个ProxyServerComponent对象代表一个服务器节点
 * 例如：若需要连接多个flyingsocks服务器实现负载均衡，则需要多个ProxyServerComponent对象
 */
public class ProxyServerComponent extends AbstractComponent<ProxyComponent> implements ProxyRequestSubscriber {

    //该服务器节点配置信息
    private final ProxyServerConfig.Node config;

    //客户端是否启动了这个节点
    private volatile boolean use;

    //该节点连接状态是否活跃
    private volatile boolean active = false;

    //连接状态
    private volatile ConnectionState connectionState = ConnectionState.NEW;

    //连接状态监听器
    private final List<ConnectionStateListener> connectionStateListeners = new CopyOnWriteArrayList<>();

    //连接线程池
    private volatile EventLoopGroup loopGroup;

    //通用Netty引导对象
    private volatile Bootstrap bootstrap;

    //与flyingsocks服务器会话对象
    private volatile ProxyServerSession proxyServerSession;

    //流量监测器
    private volatile TrafficCounter trafficCounter;

    //下次重连的时间(绝对时间戳)
    private volatile long nextReconnectTime = -1L;

    //活跃的代理请求Map
    private final ConcurrentMap<Integer, SerialProxyRequest> activeProxyRequestMap = new ConcurrentHashMap<>(512);

    //代理请求ID生成器
    private final AtomicInteger serialBuilder = new AtomicInteger(0);

    /**
     * @param proxyComponent 父组件引用
     * @param config         该FS服务器的配置对象
     */
    public ProxyServerComponent(ProxyComponent proxyComponent, ProxyServerConfig.Node config) {
        super(generalName(config.getHost(), config.getPort()), Objects.requireNonNull(proxyComponent));
        this.config = Objects.requireNonNull(config);
        this.use = config.isUse();
    }


    private static final class SerialProxyRequest {

        private final int serialId;

        private final ProxyRequest request;

        SerialProxyRequest(int serialId, ProxyRequest request) {
            this.serialId = serialId;
            this.request = request;
        }

        public String getHost() {
            return request.getHost();
        }

        public int getPort() {
            return request.getPort();
        }

        public boolean isClose() {
            return request.isClose();
        }

        public void close() {
            this.request.close();
        }

        public void setClientMessageReceiver(MessageReceiver receiver) throws IOException {
            request.setClientMessageReceiver(receiver);
        }

        public void sendMessage(ByteBuf message) {
            Objects.requireNonNull(message);
            request.clientChannel().writeAndFlush(message);
        }

        public ProxyRequest.Protocol protocol() {
            return request.protocol();
        }
    }


    /**
     * 代理服务器连接初始化逻辑，执行步骤为:
     * 1.获取与服务器的加密方式，如果需要SSL加密，则建立一个证书连接接收服务器证书
     * 2.收取证书后，与服务器建立正式的代理连接
     */
    @Override
    protected void initInternal() {
        ConfigManager<?> cm = getConfigManager();
        GlobalConfig cfg = cm.getConfig(GlobalConfig.NAME, GlobalConfig.class);

        EncryptProvider provider;
        //目前仅支持OpenSSL加密和不加密(测试性质)
        if (config.getEncryptType() == EncryptType.NONE) {
            provider = null;
        } else if (config.getEncryptType() == EncryptType.SSL) {
            updateConnectionState(ConnectionState.SSL_INITIAL);
            final Thread thread = Thread.currentThread();
            final String host = config.getHost();
            final int certPort = config.getCertPort();
            //尝试从服务器上获取SSL证书
            final EventLoopGroup sslGroup = parent.createNioEventLoopGroup(1);
            Bootstrap sslBoot = createSslServiceBootstrap(cfg);
            updateConnectionState(ConnectionState.SSL_CONNECTING);
            //连接到服务器的证书端口
            sslBoot.connect(host, certPort).addListener(f -> {
                if (!f.isSuccess()) {
                    Throwable t = f.cause();
                    if (t instanceof ConnectTimeoutException) {
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

            if (!connectionState.isNormal()) {
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

        Map<String, Object> params = new HashMap<>(2);
        params.put("alloc", PooledByteBufAllocator.DEFAULT);

        loopGroup = parent.createNioEventLoopGroup(1);
        bootstrap = new Bootstrap()
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, cfg.getConnectTimeout())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline cp = ch.pipeline();
                        ChannelTrafficShapingHandler trafficHandler = new ChannelTrafficShapingHandler(1000L);
                        cp.addLast(trafficHandler);
                        ProxyServerComponent.this.trafficCounter = trafficHandler.trafficCounter();;

                        cp.addLast(new ChannelTrafficShapingHandler(1000L));
                        if (provider != null) {
                            if (!provider.isInboundHandlerSameAsOutboundHandler()) {
                                cp.addLast(provider.encodeHandler(params));
                            }
                            cp.addLast(provider.decodeHandler(params));
                        }

                        cp.addLast(new InitialHandler());
                    }
                });

        super.initInternal();
    }


    private Bootstrap createSslServiceBootstrap(GlobalConfig cfg) {
        if (config.getEncryptType() != EncryptType.SSL) {
            throw new IllegalStateException();
        }

        final byte[] md5 = calcuateCertFileMD5(cfg.configPath());
        if (md5 == null) {
            throw new ComponentException();
        }

        final Thread thread = Thread.currentThread();
        final String host = config.getHost();
        final int certPort = config.getCertPort();
        //尝试从服务器上获取SSL证书
        final EventLoopGroup sslGroup = parent.createNioEventLoopGroup(1);
        Bootstrap sslBoot = new Bootstrap()
                .group(sslGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, cfg.getConnectTimeout());

        sslBoot.handler(new ChannelInitializer<SocketChannel>() {
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
                            case SIMPLE:
                                msg = new CertRequestMessage(AuthMethod.SIMPLE, md5);
                                break;
                            case USER:
                                msg = new CertRequestMessage(AuthMethod.USER, md5);
                                break;
                            default:
                                throw new IllegalArgumentException("Auth method: " + config.getAuthType() + " Not support.");
                        }

                        List<String> keys = msg.getAuthMethod().getContainsKey();
                        for (String key : keys) {
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
                        if (msg.needUpdate()) {
                            InputStream is = msg.getFile();
                            int len = msg.getLength();
                            writeCertFile(cfg.configPath(), is, len);
                            if (log.isInfoEnabled())
                                log.info("Update cert file from remote flyingsocks server {}:{}", host, certPort);

                        } else if (log.isTraceEnabled()) {
                            log.trace("Remote server cert file is same as local, from {}:{}", host, certPort);
                        }

                        success = true;
                        ctx.close();
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        if (success) {
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
        return sslBoot;
    }


    @Override
    protected void startInternal() {
        if (!connectionState.isNormal() || connectionState == ConnectionState.UNUSED) {
            return;
        }

        if (isUse()) {
            doConnect(true);
        }

        getConfigManager().registerConfigEventListener(new ConfigRemovedListener());

        super.startInternal();

        if (isActive()) {
            parent.addActiveProxyServer(this);
        }
    }


    /**
     * 更新连接状态
     *
     * @param state 状态
     */
    private void updateConnectionState(ConnectionState state) {
        this.connectionState = state;
        for (ConnectionStateListener listener : connectionStateListeners) {
            listener.connectionStateChanged(config, state);
        }
    }

    /**
     * 注册连接状态变更监听器
     * @param listener 监听器
     */
    public void registerConnectionStateListener(ConnectionStateListener listener) {
        Objects.requireNonNull(listener);
        if (connectionStateListeners.contains(listener)) {
            return;
        }
        connectionStateListeners.add(listener);
    }

    /**
     * 删除连接状态变更监听器
     */
    public void removeConnectionStateListener(ConnectionStateListener listener) {
        connectionStateListeners.remove(listener);
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

    @Override
    public Set<ProxyRequest.Protocol> requestProtocol() {
        return ProxyRequestSubscriber.ANY_PROTOCOL;
    }

    private void doConnect(boolean sync) {
        if (active) {
            throw new IllegalStateException("This component has been connect.");
        }

        updateConnectionState(ConnectionState.PROXY_INITIAL);
        String host = config.getHost();
        int port = config.getPort();

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
                    nextReconnectTime = -1L;
                    //连接成功后注册
                    parent.registerSubscriber(ProxyServerComponent.this);
                    f.removeListener(this);
                } else {
                    Throwable t = future.cause();
                    if (log.isWarnEnabled()) {
                        log.warn("Can not connect to flyingsocks server, cause:", t);
                    }

                    if (t instanceof ConnectTimeoutException) {
                        updateConnectionState(ConnectionState.PROXY_CONNECT_TIMEOUT);
                    } else {
                        updateConnectionState(ConnectionState.PROXY_CONNECT_ERROR);
                    }

                    f.removeListener(this);

                    if (connectionState.canRetry()) {
                        afterChannelInactive(); //重新尝试连接
                    }
                }

                if (sync) {
                    waitLatch.countDown();
                }
            }
        });

        if (sync) {
            try {
                waitLatch.await(10000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.warn("ProxyServerComponent interrupted when synchronize doConnect");
            }
        }
    }


    @Override
    protected void stopInternal() {
        log.info("Ready to stop ProxyServerComponent {}:{}...", config.getHost(), config.getPort());

        updateConnectionState(ConnectionState.UNUSED);
        active = false;
        parent.removeSubscriber(this);
        parent.removeProxyServer(this);
        getConfigManager().removeConfig(OpenSSLConfig.generalName(config.getHost(), config.getPort()));

        if (loopGroup != null) {
            loopGroup.shutdownGracefully().addListener(future -> {
                if (future.isSuccess()) {
                    active = false;
                } else {
                    Throwable t = future.cause();
                    if (log.isWarnEnabled()) {
                        log.warn("Shutdown Component " + getName() + "Failure, cause:", t);
                    }
                }
            });
        }

        connectionStateListeners.clear();
        activeProxyRequestMap.clear();
        super.stopInternal();

        log.info("Stop ProxyServerComponent {}:{} complete.", config.getHost(), config.getPort());
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


    /**
     * @return 上行吞吐量，单位字节每秒
     */
    public long queryUploadThroughput() {
        TrafficCounter counter = this.trafficCounter;
        return counter != null ? counter.lastWriteThroughput() : 0;
    }

    /**
     * @return 下行吞吐量，单位字节每秒
     */
    public long queryDownloadThroughput() {
        TrafficCounter counter = this.trafficCounter;
        return counter != null ? counter.lastReadThroughput() : 0;
    }


    public void setUse(boolean use) {
        this.use = use;
    }

    /**
     * 连接失效后的处理逻辑
     */
    private synchronized void afterChannelInactive() {
        if (log.isInfoEnabled())
            log.info("Disconnect with flyingsocks server {}:{}", config.getHost(), config.getPort());

        parent.removeSubscriber(this); //移除订阅，防止在此期间请求涌入队列
        active = false;

        loopGroup.shutdownGracefully();

        activeProxyRequestMap.values().forEach(SerialProxyRequest::close);
        activeProxyRequestMap.clear();

        if (!connectionState.isNormal() && !connectionState.canRetry()) {
            return;
        }

        TrafficCounter trafficCounter = this.trafficCounter;
        if (trafficCounter != null) {
            trafficCounter.stop();
            this.trafficCounter = null;
        }

        //处理掉线重连
        //如果父组件没有处于正在停止状态并且用户还希望继续使用该节点
        if (!parent.getState().after(LifecycleState.STOPING) && use) {
            long time = this.nextReconnectTime;
            if (time == -1L) {
                nextReconnectTime = time = 2 * 1000;
            } else {
                nextReconnectTime = time = time >= 30000 ? 30000 : time * 2;
            }

            if (time > 2000L) {
                if (log.isInfoEnabled())
                    log.info("Waiting {}ms before reconnect server {}:{}", time, config.getHost(), config.getPort());
                Thread.interrupted(); //上述clientMessageProcessor和loopGroup调用了
                try {
                    wait(time);
                } catch (InterruptedException ignore) {
                }
                //wait会释放锁所以再一次检查
                if (parent.getState().after(LifecycleState.STOPING) || !use) {
                    return;
                }
            }

            if (log.isInfoEnabled())
                log.info("Retry to connect flyingsocks server {}:{}", config.getHost(), config.getPort());
            this.proxyServerSession = null;
            this.loopGroup = parent.createNioEventLoopGroup(1);
            doConnect(false);
        }
    }

    /**
     * 计算证书文件MD5值
     *
     * @param location 存放证书文件的路径
     * @return MD5值，若文件不存在则返回一个长度为16，值全部为0的byte数组
     */
    private byte[] calcuateCertFileMD5(Path location) {
        Path folder = location.resolve(OpenSSLConfig.folderName(config.getHost(), config.getPort()));

        if (!Files.exists(folder)) {
            try {
                Files.createDirectories(folder);
                return new byte[16];
            } catch (IOException e) {
                log.error("Can not create folder at {}", folder);
                return null;
            }
        } else if (Files.isRegularFile(folder)) {
            try {
                Files.delete(folder);
            } catch (IOException e) {
                log.error("Location {} exists a file and can not delete.", folder, e);
                return null;
            }
        }

        if (Files.isDirectory(folder)) {
            Path file = folder.resolve(OpenSSLConfig.CERT_FILE_NAME);
            if (!Files.exists(file)) {
                return new byte[16];
            }

            if (Files.isDirectory(file)) {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    log.error("location {} exists a folder and can not delete.", file, e);
                    return null;
                }
            }

            try (FileInputStream fis = new FileInputStream(file.toFile())) {
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


    private void writeCertFile(Path location, final InputStream is, final int len) throws IOException {
        Path path = location.resolve(OpenSSLConfig.folderName(config.getHost(), config.getPort())).resolve(OpenSSLConfig.CERT_FILE_NAME);
        byte[] b = new byte[len];
        int r = is.read(b);
        if (r != len) {
            throw new IOException("File real size is different from server message");
        }

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            fos.write(b);
            fos.flush();
        }
    }


    private final class InitialHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws SerializationException {
            log.trace("Start flyingsocks server connection initialize");
            updateConnectionState(ConnectionState.PROXY_CONNECT);
            ProxyServerSession session = new ProxyServerSession((SocketChannel) ctx.channel());

            Random random = new Random();
            byte[] delimiter = new byte[DelimiterMessage.DEFAULT_SIZE];
            random.nextBytes(delimiter);

            DelimiterMessage msg = new DelimiterMessage(delimiter);

            session.setDelimiter(delimiter);
            ProxyServerComponent.this.proxyServerSession = session;

            ctx.writeAndFlush(msg.serialize(ctx.alloc()));
            ctx.fireChannelActive();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object buf) throws Exception {
            if (buf instanceof ByteBuf) {
                try {
                    channelRead0(ctx, (ByteBuf) buf);
                } finally {
                    ReferenceCountUtil.release(buf);
                }
            } else {
                ctx.fireChannelRead(buf);
            }
        }


        private void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws SerializationException {
            DelimiterMessage dmsg = new DelimiterMessage(msg);
            byte[] b = dmsg.getDelimiter();
            byte[] rb = proxyServerSession.getDelimiter();

            if (rb == null) {
                log.info("Delimiter message magic number is not correct");
                ctx.close();
                return;
            }

            for (int i = 0; i < DelimiterMessage.DEFAULT_SIZE; i++) {
                if (b[i] != rb[i]) {
                    log.debug("Channel close because of delimiter is difference");
                    ctx.close();
                    return;
                }
            }

            ctx.pipeline().remove(this)
                    .addLast(new DelimiterBasedFrameDecoder(102400, Unpooled.wrappedBuffer(b)))
                    .addLast(new DelimiterOutboundHandler(proxyServerSession.getDelimiter()))
                    .addLast(FSMessageChannelOutboundHandler.INSTANCE)
                    .addLast(new AuthHandler());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            updateConnectionState(ConnectionState.PROXY_CONNECT_ERROR);
            log.warn("ProxyServerComponent occur a error", cause);
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            updateConnectionState(ConnectionState.PROXY_DISCONNECT);  //在初始化过程中有可能因为网络波动断开连接
            afterChannelInactive();
        }
    }


    private final class AuthHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            AuthMessage msg;
            switch (config.getAuthType()) {
                case SIMPLE:
                    msg = new AuthMessage(AuthMethod.SIMPLE);
                    break;
                case USER:
                    msg = new AuthMessage(AuthMethod.USER);
                    break;
                default:
                    throw new IllegalArgumentException("Auth method: " + config.getAuthType() + " Not support.");
            }

            List<String> keys = msg.getAuthMethod().getContainsKey();
            for (String key : keys) {
                msg.putContent(key, config.getAuthArgument(key));
            }

            ctx.writeAndFlush(msg, ctx.voidPromise());
            ctx.pipeline().remove(this).addLast(new ProxyHandler());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (!ctx.channel().isActive()) {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("[%s:%d]AuthHandler occur a exception", config.getHost(), config.getPort()), cause);
                }
                updateConnectionState(ConnectionState.PROXY_CONNECT_ERROR);
                afterChannelInactive();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.trace("Auth failure, from server {}:{}", config.getHost(), config.getPort());

            updateConnectionState(ConnectionState.PROXY_CONNECT_AUTH_FAILURE);  //多半是认证失败
            afterChannelInactive();
        }
    }


    private final class ProxyHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            ctx.pipeline().addFirst(new IdleStateHandler(15, 0, 0));
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                ctx.writeAndFlush(new PingMessage(), ctx.voidPromise());
                return;
            }
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof IOException && log.isInfoEnabled()) {
                updateConnectionState(ConnectionState.PROXY_DISCONNECT);
                log.info("flyingsocks server {}:{} force closure of connections", config.getHost(), config.getPort());
            } else if (log.isWarnEnabled()) {
                updateConnectionState(ConnectionState.PROXY_CONNECT_ERROR);
                log.warn(String.format("flyingsocks server connection %s:%d occur a exception",
                        config.getHost(), config.getPort()), cause);
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof ByteBuf) {
                try {
                    ByteBuf buf = (ByteBuf) msg;
                    byte head = buf.getByte(0);
                    if (head == ProxyResponseMessage.HEAD) {
                        processProxyResponseMessage(ctx, buf);
                    } else if (head == PingMessage.HEAD) {
                        processPingMessage(ctx, buf);
                    }
                } finally {
                    ReferenceCountUtil.release(msg);
                }
            } else {
                ctx.fireChannelRead(msg);
            }
        }

        private void processProxyResponseMessage(ChannelHandlerContext ctx, ByteBuf buf) {
            ProxyResponseMessage response;
            try {
                response = new ProxyResponseMessage(buf);
            } catch (SerializationException e) {
                log.warn("Serialize ProxyResponseMessage error", e);
                ctx.close();
                return;
            }

            if (response.getState() == ProxyResponseMessage.State.SUCCESS) {
                SerialProxyRequest request = activeProxyRequestMap.get(response.serialId());
                if (request == null) {
                    buf.release();
                    //log.warn("Unable get active request [serialId:{}]", response.serialId());
                    return;
                }

                request.sendMessage(response.getMessage());
            }
        }


        protected void processPingMessage(ChannelHandlerContext ctx, ByteBuf buf) {
            try {
                PingMessage msg = new PingMessage();
                msg.deserialize(buf);
                PongMessage pong = new PongMessage();
                ctx.writeAndFlush(pong, ctx.voidPromise());
            } catch (SerializationException e) {
                log.warn("Illegal PingMessage", e);
                ctx.close();
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
        try {
            req.setClientMessageReceiver(new ClientMessageReceiver(req));
        } catch (IOException e) {
            if (!req.isClose()) {
                req.close();
            }
        }
    }


    private final class ClientMessageReceiver implements MessageReceiver {

        private final SerialProxyRequest request;

        ClientMessageReceiver(SerialProxyRequest request) {
            this.request = Objects.requireNonNull(request);
            activeProxyRequestMap.put(request.serialId, request);
        }

        @Override
        public void receive(ByteBuf buf) {
            if (proxyServerSession == null) {
                buf.release();
                request.close();
                return;
            }

            ProxyRequestMessage prm = new ProxyRequestMessage(request.serialId,
                    request.protocol().toMessageType());

            prm.setHost(request.getHost());
            prm.setPort(request.getPort());
            prm.setMessage(buf);

            SocketChannel channel = proxyServerSession.socketChannel();
            channel.writeAndFlush(prm, channel.voidPromise());
        }

        @Override
        public void close() {
            activeProxyRequestMap.remove(request.serialId);
        }
    }

    /**
     * 负责监听配置被移除时(用户删除正在使用的FS服务器)停止当前组件
     */
    private final class ConfigRemovedListener implements ConfigEventListener {
        @Override
        public void configEvent(ConfigEvent event) {
            if (event.getEvent().equals(Config.UPDATE_EVENT) && event.getSource() instanceof ProxyServerConfig) {
                ProxyServerConfig psc = (ProxyServerConfig) event.getSource();
                if (!psc.containsProxyServerNode(config)) {
                    synchronized (ProxyServerComponent.this) {
                        if (!getState().after(LifecycleState.STOPING)) {
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

    public static String generalName(String host, int port) {
        return String.format("ProxyServerComponent-%s:%d", host, port);
    }

}
