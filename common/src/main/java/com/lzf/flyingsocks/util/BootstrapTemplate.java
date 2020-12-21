package com.lzf.flyingsocks.util;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author lzf abc123lzf@126.com
 * @since 2020/12/17 12:55
 */
public final class BootstrapTemplate {

    private final Bootstrap bootstrap;

    public BootstrapTemplate(Bootstrap template) {
        this.bootstrap = Objects.requireNonNull(template);
    }

    public Bootstrap newInstance(Consumer<SocketChannel> channelInitializer) {
        Bootstrap b = bootstrap.clone();
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                channelInitializer.accept(ch);
            }
        });

        return b;
    }

    public void doConnect(String host, int port, Consumer<SocketChannel> channelInitializer,
                          GenericFutureListener<ChannelFuture> future) {
        Bootstrap b = newInstance(channelInitializer);
        b.connect(host, port).addListener(future);
    }


}
