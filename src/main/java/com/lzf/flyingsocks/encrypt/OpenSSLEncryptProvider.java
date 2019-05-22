package com.lzf.flyingsocks.encrypt;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;

import java.util.Map;

public class OpenSSLEncryptProvider implements EncryptProvider {

    @Override
    public boolean isInboundHandlerSameAsOutboundHandler() {
        return false;
    }

    @Override
    public ChannelInboundHandler decodeHandler(Map<String, String> params) throws Exception {
        return null;
    }

    @Override
    public ChannelOutboundHandler encodeHandler(Map<String, String> params) throws Exception {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }
}
