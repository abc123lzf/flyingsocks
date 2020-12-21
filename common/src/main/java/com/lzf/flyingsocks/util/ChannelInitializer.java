package com.lzf.flyingsocks.util;

import io.netty.channel.socket.SocketChannel;

/**
 * @author lzf abc123lzf@126.com
 * @since 2020/12/18 9:38
 */
@FunctionalInterface
public interface ChannelInitializer {

    void initChannel(SocketChannel ch) throws Exception;

}
