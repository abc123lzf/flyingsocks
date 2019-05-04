package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.Client;
import io.netty.buffer.ByteBuf;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.*;

/**
 * 实现代理功能的核心组件
 * @param <M> 代理请求消息类型
 */
public abstract class ProxyComponent<M extends ProxyRequest> extends AbstractComponent<Client> {

    /**
     * 该队列起到一个通知的作用，当客户端有代理请求时，会将这个代理请求放入队列。
     * 此队列仅限放置无需进行代理的请求
     */
    private final BlockingQueue<ProxyRequest> needlessProxyRequestQueue = new LinkedBlockingQueue<>();

    /**
     * 该队列起到一个通知的作用，当客户端有代理请求时，会将这个代理请求放入队列。
     * 此队列仅限放置需要进行代理的请求
     */
    private final BlockingQueue<ProxyRequest> needProxyRequestQueue = new LinkedBlockingQueue<>();

    /**
     * 是否开启客户端的负载均衡
     */
    private volatile boolean loadBalance = false;

    /**
     * 正在启用的服务器节点
     */
    private final List<ProxyServerComponent> usingProxyServers = new CopyOnWriteArrayList<>();


    protected ProxyComponent(Client client) {
        super("ProxyCore", client);
    }

    protected ProxyComponent(Client client, String name) {
        super(name, client);
    }

    @Override
    protected void initInternal() {
        ConfigManager<?> cm = getParentComponent().getConfigManager();
        ProxyServerConfig cfg = new ProxyServerConfig(cm);
        cm.registerConfig(cfg);

        ProxyServerConfig.Node[] nodes = cfg.getProxyServerConfig();

        for(ProxyServerConfig.Node node : nodes) {
            ProxyServerComponent c = new ProxyServerComponent(this, node);
            addComponent(c);
            if(node.isUse())
                usingProxyServers.add(c);
        }

        if(!loadBalance && usingProxyServers.size() > 1) {
            throw new ComponentException(new IllegalStateException("When load balance is turn off, " +
                    "the using proxy server number must not be grater than 1"));
        }

        super.initInternal();
    }

    public void pushProxyRequest(M request) {
        //确保拥有活跃的代理服务器连接，以免造成队列没有消费者的现象
        if(request.needProxy() && usingProxyServers.size() > 0)
            needProxyRequestQueue.offer(request);
        else
            needlessProxyRequestQueue.offer(request);
    }

    @SuppressWarnings("unchecked")
    public M pollProxyRequest(boolean proxy) throws InterruptedException {
        if(proxy)
            return (M) needProxyRequestQueue.take();
        else
            return (M) needlessProxyRequestQueue.take();
    }

    @SuppressWarnings("unchecked")
    public M pollProxyRequest(boolean proxy ,long timeOut, TimeUnit timeUnit) throws InterruptedException {
        if(proxy)
            return (M) needProxyRequestQueue.poll(timeOut, timeUnit);
        else
            return (M) needlessProxyRequestQueue.poll(timeOut, timeUnit);
    }

    private void addProxyServerComponent(ProxyServerConfig cfg) {
        ProxyServerConfig.Node[] nodes = cfg.getProxyServerConfig();

        for(ProxyServerConfig.Node node : nodes) {
            ProxyServerComponent c = new ProxyServerComponent(this, node);
            addComponent(c);
            if(node.isUse())
                usingProxyServers.add(c);
        }
    }

    private final class ServerProxyConfigListener implements ConfigEventListener {
        @Override
        public void configEvent(ConfigEvent configEvent) {
            if(!(configEvent.getSource() instanceof ProxyServerConfig))
                return;

            ProxyServerConfig cfg = (ProxyServerConfig) configEvent.getSource();

            if(configEvent.getEvent().equals(Config.UPDATE_EVENT)) {

            }

        }
    }

    void setLoadBalance(boolean loadBalance) {
        //TODO 更多的处理
        this.loadBalance = loadBalance;
    }
}
