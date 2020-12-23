package com.lzf.flyingsocks.encrypt;

import com.lzf.flyingsocks.Named;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;

import java.util.Map;

/**
 * 加密、解密Handler提供者
 * 对flyingsocks客户端和服务端传输的数据进行加密
 */
public interface EncryptProvider extends Named {

    /**
     * 初始化Provider
     *
     * @param params 初始化参数
     */
    void initialize(Map<String, ?> params) throws Exception;

    /**
     * @return 返回decodeHandler方法和encodeHandler返回的是不是同一个类型
     */
    default boolean isInboundHandlerSameAsOutboundHandler() {
        return false;
    }

    /**
     * 返回解密处理器
     *
     * @param params 参数
     * @return ChannelInboundHandler实例
     * @throws Exception 实例化过程抛出的异常
     */
    ChannelInboundHandler decodeHandler(Map<String, ?> params) throws Exception;

    /**
     * 返回加密处理器
     *
     * @param params 参数
     * @return ChannelOutboundHandler实例
     * @throws Exception 实例化过程抛出的一场
     */
    ChannelOutboundHandler encodeHandler(Map<String, ?> params) throws Exception;
}
