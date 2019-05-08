package com.lzf.flyingsocks.client.proxy;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.client.Client;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * 实现代理功能的核心组件
 */
public abstract class ProxyComponent extends AbstractComponent<Client> implements ProxyRequestManager {

    /*
     * 该队列起到一个通知的作用，当客户端有代理请求时，会将这个代理请求放入队列。
     * 此队列仅限放置无需进行代理的请求
     */
    //private final BlockingQueue<ProxyRequest> needlessProxyRequestQueue = new LinkedBlockingQueue<>();

    /*
     * 该队列起到一个通知的作用，当客户端有代理请求时，会将这个代理请求放入队列。
     * 此队列仅限放置需要进行代理的请求
     */
    //private final BlockingQueue<ProxyRequest> needProxyRequestQueue = new LinkedBlockingQueue<>();


    private final List<ProxyRequestSubscriber> requestSubscribers = new CopyOnWriteArrayList<>();

    /**
     * 是否开启客户端的负载均衡
     */
    private volatile boolean loadBalance = false;

    /**
     * 正在启用的服务器节点
     */
    private final List<ProxyServerComponent> activeProxyServers = new CopyOnWriteArrayList<>();

    /**
     * 代理服务器配置
     */
    private ProxyServerConfig proxyServerConfig;

    /**
     * PAC配置
     */
    private ProxyAutoConfig proxyAutoConfig;


    protected ProxyComponent(Client client) {
        super("ProxyCore", client);
    }


    @Override
    protected void initInternal() {
        ConfigManager<?> cm = getParentComponent().getConfigManager();
        //加载PAC配置
        ProxyAutoConfig pac = new ProxyAutoConfig(cm);
        cm.registerConfig(pac);
        this.proxyAutoConfig = pac;

        //加载flyingsocks服务器配置
        ProxyServerConfig cfg = new ProxyServerConfig(cm);
        cm.registerConfig(cfg);
        this.proxyServerConfig = cfg;
        initProxyServerComponent();

        if(!loadBalance && activeProxyServers.size() > 1) {
            throw new ComponentException(new IllegalStateException("When load balance is turn off, " +
                    "the using proxy server number must not be grater than 1"));
        }

        super.initInternal();
    }

    /*public void pushProxyRequest(ProxyRequest request) {
        //根据PAC文件的配置自动选择代理模式，并且必须要持有连接成功的Flyingsocks服务器
        request.setProxy(proxyAutoConfig.needProxy(request.getHost()) && activeProxyServers.size() > 0);
        //确保拥有活跃的代理服务器连接，以免造成队列没有消费者的现象
        if(request.needProxy())
            needProxyRequestQueue.offer(request);
        else
            needlessProxyRequestQueue.offer(request);
    }

    @SuppressWarnings("unchecked")
    public ProxyRequest pollProxyRequest(boolean proxy) throws InterruptedException {
        if(proxy)
            return needProxyRequestQueue.take();
        else
            return needlessProxyRequestQueue.take();
    }

    @SuppressWarnings("unchecked")
    public ProxyRequest pollProxyRequest(boolean proxy ,long timeOut, TimeUnit timeUnit) throws InterruptedException {
        if(proxy)
            return needProxyRequestQueue.poll(timeOut, timeUnit);
        else
            return needlessProxyRequestQueue.poll(timeOut, timeUnit);
    }*/

    @Override
    public void registerSubscriber(ProxyRequestSubscriber subscriber) {
        requestSubscribers.add(subscriber);
    }

    @Override
    public void removeSubscriber(ProxyRequestSubscriber subscriber) {
        requestSubscribers.remove(subscriber);
    }

    @Override
    public void publish(ProxyRequest request) {
        //根据PAC文件的配置自动选择代理模式
        boolean np = needProxy(request.getHost());
        boolean isPub = false;
        for(int i = 0; i < requestSubscribers.size(); i++) {
            ProxyRequestSubscriber s = requestSubscribers.get(i);
            if(request.getClass() == s.requestType() &&
                    (s.receiveNeedProxy() && np || s.receiveNeedlessProxy() && !np)) {
                if (!isPub) {
                    s.receive(request);
                    isPub = true;
                } else {
                    try {
                        s.receive((ProxyRequest) request.clone());
                    } catch (CloneNotSupportedException e) {
                        throw new Error(e);
                    }
                }
            }
        }
    }

    protected boolean needProxy(String host) {
        return proxyAutoConfig.needProxy(host);
    }

    /**
     * 添加已经成功连接flyingsocks服务器
     * @param component ProxyServerComponent实例
     */
    void addActiveProxyServer(ProxyServerComponent component) {
        Objects.requireNonNull(component);
        if(component.getParentComponent() != this) {
            throw new IllegalArgumentException("This ProxyComponent don't have this ProxyServerComponent instance");
        }

        activeProxyServers.add(component);
    }

    void removeProxyServer(ProxyServerComponent component) {
        Objects.requireNonNull(component);
        boolean success = activeProxyServers.remove(component);
        if(success) {
            removeComponentByName(component.getName());
        }
    }

    private void initProxyServerComponent() {
        assert getState() == LifecycleState.INITIALIZING;
        ProxyServerConfig.Node[] nodes = proxyServerConfig.getProxyServerConfig();

        for(ProxyServerConfig.Node node : nodes) {
            if(node.isUse()) {
                ProxyServerComponent c = new ProxyServerComponent(this, node);
                addComponent(c);
            }
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
