package com.techq.available.quorum.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.nio.ServerCnxnFactory;
import com.forest.ape.server.ApeServer;
import com.techq.available.AvailableConfig;
import com.techq.available.data.BasicPacket;
import com.techq.available.quorum.Election;
import com.techq.available.quorum.Notification;


public class Leader implements Learner {
	private static final Logger LOG = LoggerFactory.getLogger(Leader.class);
	LinkedBlockingQueue<Notification> rubbishPacket = new LinkedBlockingQueue<Notification>();
	volatile boolean reachQuorums = false;
	volatile long tick = 0;
	long leaderId = -1;
	volatile long expectClock = -1;
	int quorumSize = 0;
	boolean isWorkStarted = false;
	int pingTick = 0;
	volatile boolean hasPrepared = false;
	int correctDeviation = 0;
	ServerSocket ss;
	Set<Long> followers;
	Set<Long> quorums = new HashSet<Long>();
	private Set<LearnerHandler> learnerHandlers = new HashSet<LearnerHandler>();
	Leader self;
	DataHandler dataHandler;
	InetSocketAddress address;
	@Override
	public void addPackets(BasicPacket packet) {
		dataHandler.mqQueue.add(packet);
	}
	
	
	class LearnerCnxAcceptor extends Thread {
		private volatile boolean stop = false;
		
		@Override
		public void run() {
			try {
				LOG.info("leader[id=" +leaderId + "] listen on:" + ss.getLocalPort());
				while (!stop) {
					try {
						Socket s = ss.accept();
						s.setSoTimeout(AvailableConfig.tickTime * AvailableConfig.connectedTick);
						s.setTcpNoDelay(true);
						LearnerHandler fh = new LearnerHandler(s, self, leaderId);
						fh.start();
					} catch (SocketException e) {
						if (stop) {
							LOG.info("exception while shutting down acceptor: " + e);
							// When Leader.shutdown() calls ss.close(),
							// the call to accept throws an exception.
							// We catch and set stop to true.
							stop = true;
						} else {
							throw e;
						}
					}
				}
			} catch (Exception e) {
				LOG.warn("Exception while accepting follower", e);
			}
		}

		public void halt() {
			stop = true;
			if (this.isAlive())
				this.interrupt();
			LOG.debug("halt accepter");
		}
	}
	
	
	LearnerCnxAcceptor acceptor;
	
	
	public Leader(long leaderId, Set<Long> followers, InetSocketAddress addr) throws Exception {
		self = this;
		ss = new ServerSocket(addr.getPort());
		this.address = new InetSocketAddress(AvailableConfig.CLIENT_SERVING_IP, AvailableConfig.clientPorts.get(leaderId));
		tick = 0;
		this.leaderId = leaderId;
		this.followers = followers;
		reachQuorums = true;
		dataHandler = new DataHandler(this);
		quorumSize = followers.size();
	}

	public boolean leading() throws IOException, InterruptedException{
		
		//wait for the followers
		acceptor = new LearnerCnxAcceptor();
		acceptor.start();
		
		/**
		 * reach quorums first
		 */
		
		this.tick = 0;
		while(quorums.size() * 2 > followers.size()) {

			if (tick >= AvailableConfig.connectedTick) {
				LOG.error("restart looking for leader, reason: can't reach quorums before started");
				shutdown();
				return false;
			}
			TimeUnit.MILLISECONDS.sleep(AvailableConfig.tickTime);
			this.tick++;
		}
		
		for (LearnerHandler leaner : learnerHandlers) {
			leaner.start();
		}
		dataHandler.start();
		this.tick = 0;
		int time = 0;
		
		
		ServerCnxnFactory factory = ServerCnxnFactory.createFactory();
		factory.configure(this.address, AvailableConfig.MAX_CLIENT_CNXN);
		try {
			factory.setServer(new ApeServer(this));
			factory.start();
		} catch (Exception e) {
			LOG.error("error happened when starting serving for clients, caused:", e);
			factory.shutdown();
			shutdown();
			return false;
		}
		
		
		while (reachQuorums) {
			TimeUnit.MILLISECONDS.sleep(AvailableConfig.tickTime);
			int cnt = 1;
			for (LearnerHandler leaner : learnerHandlers) {
				if (leaner.sync(this.tick)) {
					cnt++;
					//leaner.correct(this.tick);
				}
			}
			if (cnt * 2 < quorumSize){
				LOG.error("restart looking for leader, reason: expect quorum num is : " + quorumSize
						+ ", but now it is " + cnt);
				shutdown();
				return false;
			} else {
				LOG.info("sync time:" + (++time) + ", tick:" + this.tick + ", quorumNum:" + cnt);
			}

			this.tick++;
		}
		shutdown();
		if (reachQuorums == true && this.tick < AvailableConfig.syncTick)
			return true;
		return false;
	}

	public void shutdown() throws IOException {
		LOG.warn("#shutdown");
		if (!ss.isClosed()) {
			ss.close();
		}
		if (acceptor.isAlive()) {
			acceptor.halt();
		}
		
		if (dataHandler.isAlive())
			dataHandler.halt();
		
		
		CopyOnWriteArraySet<LearnerHandler> copyLeaders = 
			new CopyOnWriteArraySet<LearnerHandler>(learnerHandlers);
		for (LearnerHandler leader : copyLeaders) {
			leader.shutdown();
		}
	}

	class PreparedWorker extends Thread {
		volatile boolean isRunning = true;
		Election election;
		PreparedWorker(Election election) {
			this.election = election;
		}
		
		
		public void run() {
			while(isRunning) {
				Set<Long> quorumSids = new HashSet<Long>();
				quorumSids.add(leaderId);
				try {
					Notification n = this.election.pollPing(AvailableConfig.pollTimeout, AvailableConfig.pollTimeUnit);
					if (n == null || n != null && !followers.contains(n.getSid())) {
						if (n != null) { 
							LOG.warn("rubbish message find in preparing:" + n);
						}
						continue;
					}
					if (n.getType().equals(Notification.mType.AGREEMENT) && n.getLeader() == leaderId) {
						quorumSids.add(n.getFrom());
						n.setType(Notification.mType.CONFIRM);
						n.setFrom(leaderId);
						this.election.offerACK(n);
						if (quorumSids.size() * 2 > quorumSize) {
							hasPrepared = true;
							isRunning = false;
							break;
						}
					} else {
						LOG.warn("rubbish message find in preparing:" + n);
					}
				} catch (InterruptedException e) {
					e.printStackTrace(System.out);
				}
			}
		}
		
		public void finish() {
			if (!isRunning)
				return;
			isRunning = false;
			this.interrupt();
		}
	}
	

	public void addLearnerHandler(LearnerHandler learnerHandler) {
		synchronized (learnerHandlers ) {
			learnerHandlers.add(learnerHandler);
		}
	}
	
	
	public void removeLearnerHandler(LearnerHandler learnerHandler) {
		synchronized (learnerHandlers ) {
			learnerHandlers.remove(learnerHandler);
		}
	}
	
	@Override
	public boolean isLeader() {
		return true;
	}
	
	@Override
	public long getId() {
		return leaderId;
	}

	
	
}
