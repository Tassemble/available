package com.forest.ape.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.server.ApeServer;
import com.forest.ape.server.Server;

/**
 * 
 * @author CHQ 2012-2-8
 */
public class NettyServerCnxnFactory extends ServerCnxnFactory {
	Logger LOG = LoggerFactory.getLogger(NettyServerCnxnFactory.class);

	/**
	 * we can know how many clients is connected now
	 */
	ChannelGroup allChannels = new DefaultChannelGroup("ApeChinnels");
	ServerBootstrap bootstrap;
	Channel parentChannel;
	CnxnChannelHandler channelHandler = new CnxnChannelHandler();
	Server server;
	HashSet<ServerCnxn> cnxns = new HashSet<ServerCnxn>();
	HashMap<InetAddress, Set<NettyServerCnxn>> ipMap = new HashMap<InetAddress, Set<NettyServerCnxn>>();
	InetSocketAddress localAddress;
	int maxClientCnxns = 60;
	AtomicLong sessionCnt = new AtomicLong(0);

	@Sharable
	class CnxnChannelHandler extends SimpleChannelHandler {

		@Override
		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
				throws Exception {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Channel closed " + e);
			}
			allChannels.remove(ctx.getChannel());

		}

		@Override
		public void channelConnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) throws Exception {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Channel connected " + e);
			}
			allChannels.add(ctx.getChannel());
			NettyServerCnxn cnxn = new NettyServerCnxn(ctx.getChannel(),
					server, NettyServerCnxnFactory.this);
			cnxn.setSessionId(sessionCnt.addAndGet(1));
			ctx.setAttachment(cnxn);
			addCnxn(cnxn);

		}

		@Override
		public void channelDisconnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) throws Exception {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Channel disconnected " + e);
            }
            NettyServerCnxn cnxn = (NettyServerCnxn) ctx.getAttachment();
            if (cnxn != null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Channel disconnect caused close " + e);
                }
                cnxn.close();
            }
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
				throws Exception {
            LOG.warn("Exception caught " + e, e.getCause());
            NettyServerCnxn cnxn = (NettyServerCnxn) ctx.getAttachment();
            if (cnxn != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Closing " + cnxn);
                    cnxn.close();
                }
            }
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
				throws Exception {
			if (LOG.isTraceEnabled()) {
				LOG.trace("message received called " + e.getMessage());
			}
			try {
				if (LOG.isDebugEnabled()) {
					LOG.debug("New message " + e.toString() + " from "
							+ ctx.getChannel());
				}

				NettyServerCnxn cnxn = (NettyServerCnxn) ctx.getAttachment();
				synchronized (cnxn) {
					cnxn.receiveMessage((ChannelBuffer) e.getMessage());
				}

			} catch (Exception ex) {
				LOG.error("Unexpected exception in receive", ex);
				throw ex;
			}

		}

		ByteBuffer bb = null;

		private void processMessage(MessageEvent e, NettyServerCnxn cnxn) {

		}

		private void increasePacketReceived() {
			// TODO Auto-generated method stub

		}

		@Override
		public void writeComplete(ChannelHandlerContext ctx,
				WriteCompletionEvent e) throws Exception {
			if (LOG.isTraceEnabled()) {
				LOG.trace("write complete " + e);
			}

		}
	}

	NettyServerCnxnFactory() {
		bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));
		// parent channel
		bootstrap.setOption("reuseAddress", true);
		// child channels
		bootstrap.setOption("child.tcpNoDelay", true);

		bootstrap.getPipeline().addLast("channelHandle", channelHandler);
	}

	public static void main(String[] args) {
		new NettyServerCnxnFactory();
	}

	boolean killed;

	@Override
	public void join() throws InterruptedException {
		synchronized (this) {
			while (!killed) {
				wait();
			}
		}
	}

	@Override
	public void shutdown() {
		LOG.info("shutdown called " + localAddress);

		// null if factory never started
		if (parentChannel != null) {
			parentChannel.close().awaitUninterruptibly();
			closeAll();
			allChannels.close().awaitUninterruptibly();
			bootstrap.releaseExternalResources();
		}

		if (server != null) {
			server.shutdown();
		}
		synchronized (this) {
			killed = true;
			notifyAll();
		}
	}

	@Override
	public void start() {
		LOG.info("binding to port " + localAddress);
		parentChannel = bootstrap.bind(localAddress);
	}

	@Override
	public void configure(InetSocketAddress addr, int maxClientCnxns)
			throws IOException {
		this.localAddress = addr;
		this.maxClientCnxns = maxClientCnxns;
	}

	@Override
	public int getMaxClientCnxnsPerHost() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setMaxClientCnxnsPerHost(int max) {
		// TODO Auto-generated method stub

	}

	@Override
	public void closeAll() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("closeAll()");
		}

		synchronized (cnxns) {
			// got to clear all the connections that we have in the selector
			for (NettyServerCnxn cnxn : cnxns.toArray(new NettyServerCnxn[cnxns
					.size()])) {
				try {
					cnxn.close();
				} catch (Exception e) {
					LOG.warn("Ignoring exception closing cnxn sessionid 0x"
							+ Long.toHexString(cnxn.getSessionId()), e);
				}
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("allChannels size:" + allChannels.size() + " cnxns size:"
					+ cnxns.size());
		}
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	private void addCnxn(NettyServerCnxn cnxn) {
		synchronized (cnxns) {
			cnxns.add(cnxn);
			synchronized (ipMap) {
				InetAddress addr = ((InetSocketAddress) cnxn.channel
						.getRemoteAddress()).getAddress();
				Set<NettyServerCnxn> s = ipMap.get(addr);
				if (s == null) {
					s = new HashSet<NettyServerCnxn>();
				}
				s.add(cnxn);
				ipMap.put(addr, s);
			}
		}
	}

	void removeCnxn(NettyServerCnxn cnxn) {
		synchronized (cnxns) {
			boolean isOK = cnxns.remove(cnxn);
			if (isOK)
				LOG.debug("remove from cnxns and ipmap");
			synchronized (ipMap) {
				InetAddress addr = ((InetSocketAddress) cnxn.channel
						.getRemoteAddress()).getAddress();
				Set<NettyServerCnxn> s = ipMap.get(addr);
				s.remove(cnxn);
				ipMap.put(addr, s);
			}
		}
	}

}
