package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.encrypt.EncryptProvider;
import com.lzf.flyingsocks.encrypt.EncryptSupport;
import com.lzf.flyingsocks.encrypt.JksSSLEncryptProvider;
import com.lzf.flyingsocks.encrypt.OpenSSLEncryptProvider;
import com.lzf.flyingsocks.protocol.AuthMessage;
import com.lzf.flyingsocks.protocol.DelimiterMessage;
import com.lzf.flyingsocks.protocol.ProxyRequestMessage;
import com.lzf.flyingsocks.protocol.SerializationException;

import com.lzf.flyingsocks.server.ServerConfig;
import com.lzf.flyingsocks.server.UserDatabase;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class ClientProcessor extends AbstractComponent<ProxyProcessor> {

    private ServerBootstrap serverBootstrap;


    public ClientProcessor(ProxyProcessor proxyProcessor) {
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
            default:
                throw new ComponentException("Unsupport encrypt type");
        }

        if(provider != null) {
            if (provider instanceof OpenSSLEncryptProvider) {
                ConfigManager<?> manager = parent.getParentComponent().getConfigManager();
                OpenSSLConfig cfg = new OpenSSLConfig(manager);
                manager.registerConfig(cfg);

                Map<String, Object> params = new HashMap<>();
                params.put("client", false);
                params.put("file.cert", cfg.openServerCertStream());
                params.put("file.cert.root", cfg.openRootCertStream());
                params.put("file.key", cfg.openKeyStream());

                try {
                    provider.initialize(params);
                } catch (Exception e) {
                    throw new ComponentException("Load OpenSSL Module occur a exception", e);
                }
            } else if (provider instanceof JksSSLEncryptProvider) {
                throw new ComponentException("Unsupport JKS encrypt method");
            } else {
                throw new ComponentException("Unsupport other encrypt method");
            }
        }

        ServerBootstrap boot = new ServerBootstrap();
        boot.group(getParentComponent().getConnectionReceiveWorker(), getParentComponent().getRequestProcessWorker())
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
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
                    cp.addLast(getParentComponent().clientSessionHandler());
                    cp.addLast(new FixedLengthFrameDecoder(DelimiterMessage.DEFAULT_SIZE));
                    cp.addLast(new InitialHandler());
                }
            });

        serverBootstrap = boot;
    }

    @Override
    protected void startInternal() {
        try {
            serverBootstrap.bind(getParentComponent().getPort()).addListener(future -> {
                if (!future.isSuccess()) {
                    Throwable t = future.cause();
                    if (log.isErrorEnabled())
                        log.error("Server occur a error when bind port " + getParentComponent().getPort(), t);
                    throw new ComponentException(t);
                }
            }).sync();
        } catch (InterruptedException e) {
            // Should not be happend.
        }
    }

    private final class InitialHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            byte[] key = new byte[DelimiterMessage.DEFAULT_SIZE];
            msg.readBytes(key);

            if(log.isTraceEnabled())
                log.trace("Receive DelimiterMessage from client.");

            ClientSession state = getParentComponent().getClientSession(ctx.channel());
            state.setDelimiterKey(key);

            ChannelPipeline cp = ctx.pipeline();
            cp.remove(this).remove(FixedLengthFrameDecoder.class);

            ByteBuf keyBuf = Unpooled.buffer(DelimiterMessage.DEFAULT_SIZE);
            keyBuf.writeBytes(key);
            ctx.writeAndFlush(keyBuf.copy());

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
                VoidChannelPromise vcp = new VoidChannelPromise(ctx.channel(), true);
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

            byte[] b = getParentComponent().getClientSession(ctx.channel()).getDelimiterKey();

            cp.addLast(new DelimiterBasedFrameDecoder(1024 * 1000 * 50,
                    Unpooled.buffer(DelimiterMessage.DEFAULT_SIZE).writeBytes(b)));

            cp.addLast(new ProxyHandler(clientSession));
        }

        /**
         * 对客户端认证报文进行比对
         * @param msg 客户端认证报文
         * @return 是否通过认证
         */
        private boolean doAuth(AuthMessage msg) {
            ServerConfig.Node n = getParentComponent().getServerConfig();
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
            getParentComponent().publish(task);
        }
    }
}
