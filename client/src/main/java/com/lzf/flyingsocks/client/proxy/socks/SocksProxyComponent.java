package com.lzf.flyingsocks.client.proxy.socks;

import com.lzf.flyingsocks.client.Client;
import com.lzf.flyingsocks.client.proxy.ProxyComponent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.ReferenceCountUtil;

import java.util.*;
import java.util.concurrent.*;

public class SocksProxyComponent extends ProxyComponent<SocksProxyRequest> {

    public SocksProxyComponent(Client client) {
        super(client);
    }

    private EventLoopGroup nettyWorkerLoopGroup;

    private List<ClientMessageTransferTask> transferTaskList = new CopyOnWriteArrayList<>();

    private ExecutorService clientMessageProcessor;

    @Override
    protected void initInternal() {
        int cpus = getParentComponent().getAvailableProcessors();
        nettyWorkerLoopGroup = new NioEventLoopGroup(cpus < 4 ? 4 : cpus);

        clientMessageProcessor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>());

        addComponent(new SocksReceiverComponent(this));
        addComponent(new SocksSenderComponent(this));

        super.initInternal();
    }

    @Override
    protected void startInternal() {
        List<ClientMessageTransferTask> tasks = new ArrayList<>();
        for(int i = 0; i < 4; i++) {
            ClientMessageTransferTask t = new ClientMessageTransferTask();
            tasks.add(t);
            clientMessageProcessor.submit(t);
        }

        transferTaskList.addAll(tasks);

        super.startInternal();
    }

    @Override
    protected void stopInternal() {
        super.stopInternal();
    }

    EventLoopGroup getWorkerEventLoopGroup() {
        return nettyWorkerLoopGroup;
    }

    /**
     * 将代理请求放入组件
     * @param request 代理请求
     */
    @Override
    public void pushProxyRequest(SocksProxyRequest request) {
        ClientMessageTransferTask task = transferTaskList.get(request.hashCode() % transferTaskList.size());
        task.pushProxyRequest(request);
        super.pushProxyRequest(request);
    }

    /**
     * 将代理消息放入组件
     * @param request 代理请求
     * @param buf 代理消息(代理内容)
     * @return 是否添加成功
     */
    public void pushProxyMessage(SocksProxyRequest request, ByteBuf buf) {
        request.getMessageQueue().offer(buf);
    }

    private final class ClientMessageTransferTask implements Runnable {
        private final List<SocksProxyRequest> requests = new LinkedList<>();
        private final BlockingQueue<SocksProxyRequest> newRequestQueue = new LinkedBlockingQueue<>();

        @Override
        public void run() {
            Thread t = Thread.currentThread();
            while(!t.isInterrupted()) {
                ListIterator<SocksProxyRequest> it = requests.listIterator();
                while(it.hasNext()) {
                    SocksProxyRequest req = it.next();
                    Channel sc;
                    Channel cc;
                    if((cc = req.getClientChannel()) != null && !cc.isActive()) {
                        it.remove();
                        clearProxyRequest(req);
                        continue;
                    }

                    if((sc = req.getServerChannel()) == null) {
                        continue;
                    }

                    if(!sc.isActive()) {
                        it.remove();
                        clearProxyRequest(req);
                        continue;
                    }

                    BlockingQueue<ByteBuf> queue = req.getMessageQueue();
                    ByteBuf buf;
                    try {
                        while ((buf = queue.poll(1, TimeUnit.MILLISECONDS)) != null) {
                            sc.writeAndFlush(buf);
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                SocksProxyRequest newRequest;
                try {
                    while ((newRequest = newRequestQueue.poll(2, TimeUnit.MILLISECONDS)) != null) {
                        requests.add(newRequest);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private void clearProxyRequest(SocksProxyRequest request) {
            BlockingQueue<ByteBuf> queue = request.getMessageQueue();
            ByteBuf buf;
            try {
                while ((buf = queue.poll(1, TimeUnit.MILLISECONDS)) != null) {
                    ReferenceCountUtil.release(buf);
                }
            } catch (InterruptedException e) {

            }
        }

        private void pushProxyRequest(SocksProxyRequest request) {
            newRequestQueue.offer(request);
        }
    }

    /**
     * 创建单线程线程池
     * @return 单线程线程池实例
     */
    protected static ExecutorService constructSingleExecutor() {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
    }
}
