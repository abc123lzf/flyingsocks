package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.encrypt.EncryptProvider;
import com.lzf.flyingsocks.encrypt.EncryptSupport;
import com.lzf.flyingsocks.encrypt.JksSSLEncryptProvider;
import com.lzf.flyingsocks.encrypt.OpenSSLEncryptProvider;
import com.lzf.flyingsocks.protocol.*;
import com.lzf.flyingsocks.server.ServerConfig;
import com.lzf.flyingsocks.server.UserDatabase;
import com.lzf.flyingsocks.util.FSMessageChannelOutboundHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 用于处理FS客户端连接的组件
 */
public class ClientProcessor extends AbstractComponent<ProxyProcessor> {

    /**
     * 接收客户端代理连接的引导模板
     */
    private ServerBootstrap serverBootstrap;

    /**
     * 接收客户端证书的引导模板
     */
    private ServerBootstrap certBootstrap;

    /**
     * 证书文件内容
     */
    private byte[] cert;

    /**
     * 证书文件MD5
     */
    private byte[] certMD5;


    ClientProcessor(ProxyProcessor proxyProcessor) {
        super("ClientProcessor", Objects.requireNonNull(proxyProcessor));
    }


    @Override
    protected void initInternal() {
        ServerConfig.Node node = parent.getServerConfig();
        EncryptProvider provider;
        switch (node.encryptType) {
            case OpenSSL:
                provider = EncryptSupport.lookupProvider("OpenSSL");
                break;
            case JKS:
                provider = EncryptSupport.lookupProvider("JKS");
                break;
            case None:
                provider = null;
                break;
            default: {
                log.error("Unsupport encrypt type");
                System.exit(1);
                return;
            }
        }

        if(provider != null) {
            if (provider instanceof OpenSSLEncryptProvider) {
                ConfigManager<?> manager = parent.getParentComponent().getConfigManager();
                OpenSSLConfig cfg = new OpenSSLConfig(manager);
                manager.registerConfig(cfg);

                Map<String, Object> params = new HashMap<>(8);

                try(InputStream certIs = cfg.openRootCertStream()) {
                    byte[] b = new byte[10240];
                    int r = certIs.read(b);
                    byte[] nb = new byte[r];
                    System.arraycopy(b, 0, nb, 0, r);
                    this.cert = nb;

                    MessageDigest md = MessageDigest.getInstance("MD5");
                    this.certMD5 = md.digest(cert);

                    ByteArrayInputStream bais = new ByteArrayInputStream(nb);
                    params.put("file.cert", bais);
                } catch (IOException | NoSuchAlgorithmException e) {
                    log.error("Exception occur at CA cert MD5 calcuate", e);
                    System.exit(1);
                }

                try {
                    params.put("client", false);
                    params.put("file.cert.root", cfg.openRootCertStream());
                    params.put("file.key", cfg.openKeyStream());
                } catch (IOException e) {
                    log.error("Can not open CA file stream", e);
                    System.exit(1);
                }

                try {
                    provider.initialize(params);
                } catch (Exception e) {
                    log.error("Load OpenSSL Module occur a exception", e);
                    System.exit(1);
                }
            } else if (provider instanceof JksSSLEncryptProvider) {
                log.error("Unsupport JKS encrypt method");
                System.exit(1);
            } else {
                log.error("Unsupport other encrypt method");
                System.exit(1);
            }
        }

        Map<String, Object> m = Collections.singletonMap("alloc", PooledByteBufAllocator.DEFAULT);
        final Map<String, Object> params = Collections.unmodifiableMap(m);

        ServerBootstrap boot = new ServerBootstrap();
        boot.group(parent.getConnectionReceiveWorker(), parent.getRequestProcessWorker())
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline cp = ch.pipeline();
                    if(provider != null) {
                        if(!provider.isInboundHandlerSameAsOutboundHandler())
                            cp.addLast(provider.encodeHandler(params));
                        cp.addLast(provider.decodeHandler(params));
                    }
                    cp.addLast(parent.clientSessionHandler());
                    cp.addLast(FSMessageChannelOutboundHandler.INSTANCE);
                    cp.addLast(new FixedLengthFrameDecoder(DelimiterMessage.LENGTH));
                    cp.addLast(new InitialHandler());
                }
            });

        this.serverBootstrap = boot;

        ServerBootstrap certBoot = new ServerBootstrap();
        certBoot.group(parent.getConnectionReceiveWorker(), parent.getRequestProcessWorker())
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.AUTO_CLOSE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline cp = ch.pipeline();
                        int l = CertRequestMessage.END_MARK.length;
                        ByteBuf buf = Unpooled.buffer(l);
                        buf.writeBytes(CertRequestMessage.END_MARK);
                        cp.addLast(new DelimiterBasedFrameDecoder(512 + l, buf));
                        cp.addLast(new CertRequestHandler());
                    }
                });

        this.certBootstrap = certBoot;
    }

    @Override
    protected void startInternal() {
        try {
            serverBootstrap.bind(parent.getPort()).addListener(future -> {
                if (!future.isSuccess()) {
                    Throwable t = future.cause();
                    if (log.isErrorEnabled())
                        log.error("Server occur a error when bind port " + parent.getPort(), t);
                    throw new ComponentException(t);
                }
            }).sync();

            certBootstrap.bind(parent.getCertPort()).addListener(future -> {
                if (!future.isSuccess()) {
                    Throwable t = future.cause();
                    if (log.isErrorEnabled())
                        log.error("CertServer occur a error when bind port " + parent.getPort(), t);
                    throw new ComponentException(t);
                }
            }).sync();

        } catch (InterruptedException e) {
            // Should not be happend.
        }
    }

    @Override
    protected void stopInternal() {
        super.stopInternal();
    }

    /**
     * 对客户端认证报文进行比对
     * @param msg 客户端认证报文
     * @return 是否通过认证
     */
    private boolean doAuth(AuthMessage msg) {
        ServerConfig.Node n = parent.getServerConfig();
        if(n.authType.authMethod != msg.getAuthMethod()) { //如果认证方式不匹配
            return false;
        }

        if(n.authType == ServerConfig.AuthType.SIMPLE) {
            List<String> keys = msg.getAuthMethod().getContainsKey();
            for(String key : keys) {
                if(!n.getArgument(key).equals(msg.getContent(key)))
                    return false;
            }
            return true;
        } else if(n.authType == ServerConfig.AuthType.USER) {
            String group = n.getArgument("group");
            UserDatabase db = parent.getParentComponent().getUserDatabase();

            return db.doAuth(group, msg.getContent("user"), msg.getContent("pass"));
        }

        return false;
    }


    private final class CertRequestHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(cause instanceof IOException)
                return;
            if(log.isWarnEnabled())
                log.warn("Exception occur at CertRequestHandler", cause);
            ctx.close();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            CertRequestMessage req = new CertRequestMessage(msg);
            boolean auth = doAuth(req);
            if(!auth)
                ctx.close();

            byte[] src = req.getCertMD5();
            boolean update = false;
            for(int i = 0; i < 16; i++) {
                if(certMD5[i] != src[i]) {
                    update = true;
                    break;
                }
            }

            if(update) {
                CertResponseMessage resp = new CertResponseMessage(true, cert);
                ctx.writeAndFlush(resp.serialize());
            } else {
                CertResponseMessage resp = new CertResponseMessage(false, null);
                ctx.writeAndFlush(resp.serialize());
            }
        }
    }


    private final class InitialHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws SerializationException {
            byte[] key = new byte[DelimiterMessage.DEFAULT_SIZE];
            DelimiterMessage msg = new DelimiterMessage(buf);
            if(msg.getDelimiter() == null) {
                log.info("Remote client close, cause Delimiter message magic number is not correct");
                ctx.close();
                return;
            }

            msg.getDelimiter().readBytes(key);

            if(log.isTraceEnabled())
                log.trace("Receive DelimiterMessage from client.");

            ClientSession state = parent.getClientSession(ctx.channel());
            state.setDelimiterKey(key);

            ChannelPipeline cp = ctx.pipeline();
            cp.remove(this).remove(FixedLengthFrameDecoder.class);

            DelimiterMessage resp = new DelimiterMessage(key);
            ctx.writeAndFlush(resp);

            ByteBuf keyBuf = Unpooled.buffer(DelimiterMessage.DEFAULT_SIZE);
            keyBuf.writeBytes(key);

            cp.addLast(new DelimiterOutboundHandler(keyBuf));
            cp.addLast(new DelimiterBasedFrameDecoder(102400, keyBuf));
            cp.addLast(new AuthHandler(state));
        }
    }


    private final class DelimiterOutboundHandler extends ChannelOutboundHandlerAdapter {
        private final ByteBuf delimiter;

        private DelimiterOutboundHandler(ByteBuf delimiter) {
            if(delimiter.readableBytes() != DelimiterMessage.DEFAULT_SIZE)
                throw new IllegalArgumentException("Illegal delimiter ByteBuf, request delimiter length is " + DelimiterMessage.DEFAULT_SIZE);
            this.delimiter = delimiter.copy();
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            if(msg instanceof ByteBuf) {
                ChannelPromise vcp = ctx.voidPromise();
                ctx.write(msg, vcp);
                ctx.write(delimiter.copy(), vcp);
            } else {
                ctx.write(msg, promise);
            }
        }
    }


    /**
     * 负责处理来自客户端的认证消息
     */
    private final class AuthHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final ClientSession clientSession;

        private AuthHandler(ClientSession clientSession) {
            this.clientSession = Objects.requireNonNull(clientSession, "ClientSession must not be null");
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) {
            AuthMessage msg;
            try {
                msg = new AuthMessage(buf);
            } catch (SerializationException e) {
                if(log.isInfoEnabled())
                    log.info("Deserialize occur a exception", e);
                ctx.close();
                return;
            }

            boolean auth = doAuth(msg);
            if(!auth) {
                if(log.isTraceEnabled())
                    log.trace("Auth failure, from client {}", ((SocketChannel)ctx.channel()).remoteAddress().getHostName());
                ctx.close();
                return;
            } else {
                if(log.isTraceEnabled()) {
                    log.trace("Auth success, from client {}", ((SocketChannel)ctx.channel()).remoteAddress().getHostName());
                }
            }

            clientSession.passAuth();

            ChannelPipeline cp = ctx.pipeline();
            cp.remove(this).remove(DelimiterBasedFrameDecoder.class);

            byte[] b = parent.getClientSession(ctx.channel()).getDelimiterKey();

            cp.addLast(new DelimiterBasedFrameDecoder(1024 * 1000 * 50,
                    Unpooled.buffer(DelimiterMessage.DEFAULT_SIZE).writeBytes(b)));

            cp.addLast(new ProxyHandler(clientSession));
        }
    }

    private final class ProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final ClientSession clientSession;

        private ProxyHandler(ClientSession cs) {
            this.clientSession = Objects.requireNonNull(cs, "ClientSession must not be null");
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) {
            ProxyRequestMessage msg;
            try {
                msg = new ProxyRequestMessage(buf);
            } catch (SerializationException e) {
                if(log.isWarnEnabled())
                    log.warn("Serialize request occur a exception", e);
                ctx.close();
                return;
            }

            if(log.isTraceEnabled())
                log.trace("Receiver client ProxyRequest from {}", clientSession.remoteAddress().getAddress().getHostAddress());

            ProxyTask task = new ProxyTask(msg, clientSession);
            //发布代理任务
            parent.publish(task);
        }
    }
}
