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
        super("ProxyCore", Objects.requireNonNull(client));
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

    @Override
    protected void startInternal() {
        parent.getConfigManager().registerConfigEventListener(new ServerProxyConfigListener());
        super.startInternal();
    }

    @Override
    public void removeComponentByName(String name) {
        super.removeComponentByName(name);
    }

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
        if(requestSubscribers.size() == 0)
            log.warn("No RequestSubscriber found in manager");
        //根据PAC文件的配置自动选择代理模式
        boolean np = needProxy(request.getHost());
        boolean consume = false;
        int count = 0;
        for(ProxyRequestSubscriber sub : requestSubscribers) {
            if(sub.requestType().isAssignableFrom(request.getClass()) &&
                    (sub.receiveNeedProxy() && np || sub.receiveNeedlessProxy() && !np)) {
                if(count == 0) {
                    sub.receive(request);
                } else {
                    try {
                        sub.receive((ProxyRequest) request.clone());
                    } catch (CloneNotSupportedException e) {
                        throw new Error(e);
                    }
                }

                consume = true;
                count++;
            }
        }

        if(!consume && log.isWarnEnabled())
            log.warn("ProxyRequest was not consume");
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
                ProxyServerConfig.Node[] nodes = cfg.getProxyServerConfig();
                for(ProxyServerConfig.Node node : nodes) {
                    String name = ProxyServerComponent.generalName(node.getHost(), node.getPort());
                    ProxyServerComponent psc = getComponentByName(name, ProxyServerComponent.class);

                    if(node.isUse()) { //如果用户需要建立一个代理服务器连接
                        if(psc != null) {
                            psc.stop();
                            removeComponentByName(name);
                        }

                        ProxyServerComponent newPsc = new ProxyServerComponent(ProxyComponent.this, node);
                        addComponent(newPsc);
                        newPsc.init();
                        newPsc.start();
                    } else if(psc != null) { //如果用户需要关闭这个代理服务器连接
                        psc.stop();
                        removeComponentByName(name);
                    }
                }
            }
        }
    }

}
