package com.lzf.flyingsocks.server.core;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.server.Server;
import com.lzf.flyingsocks.server.ServerConfig;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.FastThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyProcessor extends AbstractComponent<Server> implements ProxyTaskManager {

	private static final AtomicInteger ID_BUILDER = new AtomicInteger(0);

	private final Logger log;

	//ProxyProcessor ID
	private final int handlerId;

	//活跃的ClientSession Map
	private final Map<Channel, ClientSession> activeClientMap = new ConcurrentHashMap<>();

	//Boss线程
	private EventLoopGroup connectionReceiveWorker = new NioEventLoopGroup(1);

	//Worker线程池
	private EventLoopGroup requestProcessWorker = new NioEventLoopGroup();

	//绑定的端口
	private final int port;

	//最大客户端数
	private final int maxClient;

	//负责添加ClientSession的进站处理器
	private final ChannelInboundHandler clientSessionHandler;

	//配置信息
	private final ServerConfig.Node serverConfig;

	//代理任务订阅者列表
	private final List<ProxyTaskSubscriber> proxyTaskSubscribers = new CopyOnWriteArrayList<>();

	FastThreadLocal<Map<Channel, ClientSession>> localClientMap = new FastThreadLocal<Map<Channel, ClientSession>>() {
		@Override
		protected Map<Channel, ClientSession> initialValue() {
			return new WeakHashMap<>();
		}
	};


	public ProxyProcessor(Server server, ServerConfig.Node serverConfig) {
		super(serverConfig.name, server);
		this.handlerId = ID_BUILDER.incrementAndGet();
		this.port = serverConfig.port;
		this.maxClient = serverConfig.maxClient;
		this.serverConfig = serverConfig;
		this.clientSessionHandler = new ClientSessionHandler();
		this.log = LoggerFactory.getLogger(String.format("ProxyProcessor [ID:%d Port:%d]", handlerId, port));
	}

	@Override
	protected void initInternal() {
		addComponent(new ClientProcessor(this));
		addComponent(new DispatchProceessor(this));
		super.initInternal();
	}

	@Override
	protected void stopInternal() {
		connectionReceiveWorker.shutdownGracefully();
		requestProcessWorker.shutdownGracefully();
		activeClientMap.clear();
		super.stopInternal();
	}

	public final int getHandlerId() {
		return handlerId;
	}

	/**
	 * @return 该代理处理器绑定的端口
	 */
	public final int getPort() {
		return port;
	}

	/**
	 * @return 该代理处理器最大的客户端TCP连接数
	 */
	public final int getMaxClient() {
		return maxClient;
	}

	/**
	 * @return 连接请求处理线程池
	 */
	final EventLoopGroup getRequestProcessWorker() {
		return requestProcessWorker;
	}

	/**
	 * @return 连接接收线程池
	 */
	final EventLoopGroup getConnectionReceiveWorker() {
		return connectionReceiveWorker;
	}

	/**
	 * @param clientSession 客户端会话对象
	 */
	private void putClientSession(ClientSession clientSession) {
		activeClientMap.put(clientSession.socketChannel(), clientSession);
	}

	/**
	 * 根据Channel获取客户端会话对象
	 * @param channel Channel通道
	 * @return 客户端会话
	 */
	final ClientSession getClientSession(Channel channel) {
		return activeClientMap.get(channel);
	}

	@Override
	public void registerSubscriber(ProxyTaskSubscriber subscriber) {
		proxyTaskSubscribers.add(subscriber);
		if(log.isInfoEnabled())
			log.info("ProxyTaskSubscriber {} has been register in manager.", subscriber.toString());
	}

	@Override
	public void removeSubscriber(ProxyTaskSubscriber subscriber) {
		if(proxyTaskSubscribers.remove(subscriber)) {
			if (log.isInfoEnabled())
				log.info("ProxyTaskSubscriber {} has been remove from manager.", subscriber.toString());
		} else if(log.isWarnEnabled()) {
			log.warn("Remove failure, cause ProxyTaskSubscriber doesn't found in list.");
		}
	}

	@Override
	public void publish(ProxyTask task) {
		if(proxyTaskSubscribers.size() == 0)
			log.warn("No ProxyTaskSubscriber register.");

		int count = 0;
		for(ProxyTaskSubscriber subscriber : proxyTaskSubscribers) {
			if(count == 0) {
				subscriber.receive(task);
			} else {
				try {
					subscriber.receive(task.clone());
				} catch (CloneNotSupportedException e) {
					throw new IllegalStateException(e);
				}
			}

			count++;
		}
	}

	private void removeClientSession(Channel channel) {
		if(log.isInfoEnabled())
			log.info("Client channel {} has been removed.", channel.id().asLongText());
		activeClientMap.remove(channel);
	}

	/**
	 * 获取默认的会话管理器
	 * @return 会话管理器
	 */
	public ChannelInboundHandler clientSessionHandler() {
		return clientSessionHandler;
	}

	/**
	 * 获取当前ProxyHandler的配置信息
	 * @return 配置信息
	 */
	ServerConfig.Node getServerConfig() {
		return serverConfig;
	}

	/**
	 * 用于管理客户端连接(ClientSession)
	 */
	@ChannelHandler.Sharable
	private final class ClientSessionHandler extends ChannelInboundHandlerAdapter {
		private final int maxClient;

		private ClientSessionHandler() {
			maxClient = serverConfig.maxClient;
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			localClientMap.get().get(ctx.channel()).updateLastActiveTime();
			ctx.fireChannelRead(msg);
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) {
			if(activeClientMap.size() > maxClient) {
				log.info("Node \"{}\" Client number out of maxClient limit, value:{}", serverConfig.name, maxClient);
				ctx.close();
			}
			ClientSession state = new ClientSession(ctx.channel());
			putClientSession(state);
			localClientMap.get().put(state.socketChannel(), state);

			ctx.fireChannelActive();
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			if(cause instanceof IOException)
				log.info("Remote host close the connection");
			else if(log.isWarnEnabled())
				log.warn("An exception occur", cause);
			ctx.close();
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) {
			localClientMap.get().remove(ctx.channel());
			removeClientSession(ctx.channel());
			ctx.fireChannelInactive();
		}
	}
}
