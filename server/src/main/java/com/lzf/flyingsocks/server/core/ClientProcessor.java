package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.protocol.AuthMessage;
import com.lzf.flyingsocks.protocol.DelimiterMessage;
import com.lzf.flyingsocks.protocol.ProxyRequestMessage;
import com.lzf.flyingsocks.protocol.SerializationException;

import com.lzf.flyingsocks.server.ServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.InputStream;
import java.security.KeyStore;


public class ClientProcessor extends AbstractComponent<ProxyProcessor> {

    private ServerBootstrap serverBootstrap;

    private SSLContext sslContext;

    public ClientProcessor(ProxyProcessor proxyProcessor) {
        super("ClientProcessor", proxyProcessor);
    }

    @Override
    protected void initInternal() {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            InputStream in = getParentComponent().getParentComponent().loadResource("classpath://flyingsocks.jks");
            ks.load(in, "flyingsocks".toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, "flyingsocks".toCharArray());
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), null, null);
            this.sslContext = context;
        } catch (Exception e) {
            throw new ComponentException(e);
        }

        SSLEngine sslEngine = sslContext.createSSLEngine();

        sslEngine.setUseClientMode(false);
        sslEngine.setNeedClientAuth(false);

        ServerBootstrap boot = new ServerBootstrap();
        boot.group(getParentComponent().getConnectionReceiveWorker(), getParentComponent().getRequestProcessWorker())
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline cp = ch.pipeline();
                    cp.addLast(new SslHandler(sslEngine));
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
            if(log.isTraceEnabled())
                log.trace("Receiver DelimiterMessage from client.");
            byte[] key = new byte[DelimiterMessage.DEFAULT_SIZE];
            msg.readBytes(key);

            ClientSession state = getParentComponent().getClientSession(ctx.channel());
            state.setDelimiterKey(key);

            ChannelPipeline cp = ctx.pipeline();
            cp.remove(this).remove(FixedLengthFrameDecoder.class);

            ByteBuf keyBuf = Unpooled.buffer(DelimiterMessage.DEFAULT_SIZE);
            keyBuf.writeBytes(key);
            ctx.writeAndFlush(keyBuf.copy());

            cp.addLast(new DelimiterBasedFrameDecoder(102400, keyBuf));
            cp.addLast(new AuthHandler(state));
        }

    }

    private final class AuthHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final ClientSession clientSession;

        private AuthHandler(ClientSession clientSession) {
            this.clientSession = clientSession;
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

            ServerConfig.Node n = getParentComponent().getServerConfig();
            boolean auth = n.authType.doAuth(n, msg);
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

            ClientSession cs = getParentComponent().localClientMap.get().get(ctx.channel());
            cs.passAuth();

            ChannelPipeline cp = ctx.pipeline();
            cp.remove(this).remove(DelimiterBasedFrameDecoder.class);

            byte[] b = getParentComponent().getClientSession(ctx.channel()).getDelimiterKey();

            cp.addLast(new DelimiterBasedFrameDecoder(1024 * 1000 * 50,
                    Unpooled.buffer(DelimiterMessage.DEFAULT_SIZE).writeBytes(b)));

            cp.addLast(new ProxyHandler());
        }
    }

    private final class ProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {
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


        }
    }
}
