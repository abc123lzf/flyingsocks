package com.lzf.flyingsocks.client.proxy;

import java.util.EventListener;

/**
 * @author lzf abc123lzf@126.com
 * @since 2020/12/23 21:42
 */
public interface ConnectionStateListener extends EventListener {

    void connectionStateChanged(ProxyServerConfig.Node node, ConnectionState state);

}
