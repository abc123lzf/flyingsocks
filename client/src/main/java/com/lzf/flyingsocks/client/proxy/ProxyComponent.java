package com.lzf.flyingsocks.client.proxy;


import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.ConfigEvent;
import com.lzf.flyingsocks.ConfigEventListener;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.LifecycleState;
import com.lzf.flyingsocks.client.Client;
import com.lzf.flyingsocks.client.proxy.direct.DirectForwardComponent;
import com.lzf.flyingsocks.client.proxy.socks.SocksConfig;
import com.lzf.flyingsocks.client.proxy.socks.SocksReceiverComponent;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.Node;

/**
 * 实现代理功能的核心组件
 */
public class ProxyComponent extends AbstractComponent<Client> implements ProxyRequestManager {

    public static final String NAME = "ProxyCore";


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
    private volatile ProxyAutoChecker proxyAutoChecker;

    /**
     * 异步任务执行器
     */
    private final ExecutorService asyncTaskExecutorService = Executors.newCachedThreadPool();

    /**
     * IO线程池
     */
    private final ExecutorService ioExectuorService;


    public ProxyComponent(Client client) {
        super(NAME, Objects.requireNonNull(client));

        int cpus = getParentComponent().availableProcessors();

        ioExectuorService = new ThreadPoolExecutor(Math.max(cpus * 2, 8), Math.max(cpus * 4, 48), 15L, TimeUnit.MINUTES, new SynchronousQueue<>(),
                (r, executor) -> {
                    log.error("Can not execture more IO Task");
                    throw new RejectedExecutionException("Can not execture more IO Task");
                });
    }


    @Override
    protected void initInternal() {
        ConfigManager<?> cm = getParentComponent().getConfigManager();
        //加载PAC配置
        ProxyAutoConfig pac = new ProxyAutoConfig(cm);
        cm.registerConfig(pac);
        this.proxyAutoChecker = pac.getProxyAutoChecker();

        //加载flyingsocks服务器配置
        ProxyServerConfig psc = new ProxyServerConfig(cm);
        cm.registerConfig(psc);
        this.proxyServerConfig = psc;
        initProxyServerComponent();

        if (!loadBalance && activeProxyServers.size() > 1) {
            throw new ComponentException(new IllegalStateException("When load balance is turn off, " +
                    "the using proxy server number must not be grater than 1"));
        }

        SocksConfig sc = new SocksConfig(cm);
        cm.registerConfig(sc);

        addComponent(new SocksReceiverComponent(this));
        addComponent(new DirectForwardComponent(this));

        super.initInternal();
    }

    @Override
    protected void startInternal() {
        parent.getConfigManager().registerConfigEventListener(new ServerProxyConfigListener());
        super.startInternal();
    }

    @Override
    protected void stopInternal() {
        asyncTaskExecutorService.shutdownNow();
        super.stopInternal();
        ioExectuorService.shutdownNow();
    }

    @Override
    public void removeComponentByName(String name) {
        super.removeComponentByName(name);
    }

    /**
     * 构造EventLoopGroup
     */
    public NioEventLoopGroup createNioEventLoopGroup(int threads) {
        return new NioEventLoopGroup(threads, ioExectuorService);
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
        if (requestSubscribers.isEmpty()) {
            log.warn("No RequestSubscriber found in manager");
        }
        //根据PAC文件的配置自动选择代理模式
        boolean np = needProxy(request.getHost());
        request.setProxy(np);
        int index = 0;
        List<Integer> list = new ArrayList<>(3);
        for (ProxyRequestSubscriber sub : requestSubscribers) {
            if ((sub.receiveNeedProxy() && np || sub.receiveNeedlessProxy() && !np) &&  //false&&true || true&&false
                    sub.requestProtocol().contains(request.protocol())) {
                list.add(index);
            }
            index++;
        }

        if (list.isEmpty()) {
            request.close();
            log.warn("ProxyRequest was not consume, target server: {}", request.host);
            return;
        }

        int hash = Math.abs(request.hashCode());
        int size = list.size();
        try {
            requestSubscribers.get(list.get(hash % size)).receive(request);
        } catch (IndexOutOfBoundsException e) {
            log.warn("ProxySubscriber object changed");
            publish(request);
        }
    }


    public void registerProxyServerConnectionStateListener(String host, int port, ConnectionStateListener listener) {
        ProxyServerComponent psc = getComponentByName(ProxyServerComponent.generalName(host, port), ProxyServerComponent.class);
        if (psc != null) {
            psc.registerConnectionStateListener(listener);
        }
    }


    public long queryProxyServerUploadThroughput(String host, int port) {
        ProxyServerComponent psc = getComponentByName(ProxyServerComponent.generalName(host, port), ProxyServerComponent.class);
        return psc != null ? psc.queryUploadThroughput() : 0;
    }


    public long queryProxyServerDownloadThroughput(String host, int port) {
        ProxyServerComponent psc = getComponentByName(ProxyServerComponent.generalName(host, port), ProxyServerComponent.class);
        return psc != null ? psc.queryDownloadThroughput() : 0;
    }



    /**
     * 根据PAC配置判断是否需要进行代理
     *
     * @param host 主机名
     * @return 是否需要代理
     */
    public boolean needProxy(String host) {
        return proxyAutoChecker.needProxy(host);
    }

    /**
     * 添加已经成功连接flyingsocks服务器
     *
     * @param component ProxyServerComponent实例
     */
    void addActiveProxyServer(ProxyServerComponent component) {
        Objects.requireNonNull(component);
        if (component.getParentComponent() != this) {
            throw new IllegalArgumentException("This ProxyComponent don't have this ProxyServerComponent instance");
        }

        activeProxyServers.add(component);
    }

    /**
     * 移除flyingsocks服务器实例
     *
     * @param component ProxyServerComponent实例
     */
    void removeProxyServer(ProxyServerComponent component) {
        Objects.requireNonNull(component);
        boolean success = activeProxyServers.remove(component);
        if (success) {
            removeComponentByName(component.getName());
        }
    }

    private void initProxyServerComponent() {
        assert getState() == LifecycleState.INITIALIZING;
        Node[] nodes = proxyServerConfig.getProxyServerConfig();

        for (Node node : nodes) {
            if (node.isUse()) {
                ProxyServerComponent c = new ProxyServerComponent(this, node);
                addComponent(c);
            }
        }
    }

    private final class ServerProxyConfigListener implements ConfigEventListener {
        @Override
        public void configEvent(ConfigEvent configEvent) {
            if (!(configEvent.getSource() instanceof ProxyServerConfig)) {
                return;
            }

            ProxyServerConfig cfg = (ProxyServerConfig) configEvent.getSource();

            if (configEvent.getEvent().equals(Config.UPDATE_EVENT)) {
                Node[] nodes = cfg.getProxyServerConfig();
                for (Node node : nodes) {
                    String name = ProxyServerComponent.generalName(node.getHost(), node.getPort());
                    ProxyServerComponent psc = getComponentByName(name, ProxyServerComponent.class);

                    if (node.isUse()) { //如果用户需要建立一个代理服务器连接
                        if (psc != null) {
                            psc.stop();
                            removeComponentByName(name);
                        }

                        ProxyServerComponent newPsc = new ProxyServerComponent(ProxyComponent.this, node);
                        addComponent(newPsc);
                        asyncTaskExecutorService.submit(() -> { //异步执行，防止连接超时阻塞GUI线程
                            synchronized (newPsc) {
                                newPsc.init();
                                newPsc.start();
                            }
                        });
                    } else if (psc != null) { //如果用户需要关闭这个代理服务器连接
                        psc.setUse(false);
                        asyncTaskExecutorService.submit(psc::stop);
                        removeComponentByName(name);
                    }
                }
            }
        }
    }

}
