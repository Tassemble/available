package com.techq.available.quorum.handler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jute.BinaryInputArchive;
import org.apache.jute.BinaryOutputArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.nio.ServerCnxnFactory;
import com.forest.ape.server.ApeServer;
import com.forest.ape.server.Type;
import com.techq.available.AvailableConfig;
import com.techq.available.connector.impl.DataCnxManager;
import com.techq.available.data.BasicPacket;
import com.techq.available.quorum.DebugConfig;
import com.techq.available.quorum.Election;
import com.techq.available.quorum.Notification;
import com.techq.available.quorum.QuorumPeer;
import com.techq.available.quorum.QuorumPeer.QuorumServer;
import com.techq.available.quorum.ServerState;

/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public class Follower implements Learner {
	private static final Logger LOG = LoggerFactory.getLogger(Follower.class);
	LinkedBlockingQueue<BasicPacket> sendqueue = new LinkedBlockingQueue<BasicPacket>();
	LinkedBlockingQueue<BasicPacket> recvqueue = new LinkedBlockingQueue<BasicPacket>();
	DataCnxManager.MessageReceiver recvThread;
	DataCnxManager.MessageSender sendThread;
	long leaderId;
	long xid;
	long myId;
	long defaultClock = -1;
	volatile boolean followLeaderFailed = false;
	PINGWorker worker;
	static Follower instance = null;
	volatile boolean following = true;
	long tick = 0;
	volatile long curTick = 0;
	Socket socket;
	InetSocketAddress clientAddr;

	@Override
	public void addPackets(BasicPacket buf) {
		sendqueue.add(buf);
	}
	
	public Follower(long leaderId, long xid, long myId, InetSocketAddress addr) throws IOException, InterruptedException {
		super();
		this.leaderId = leaderId;
		this.xid = xid;
		this.myId = myId;
		LOG.info("follower[id="+myId+"] connnect to leader[id="+leaderId+"] port:" + addr.getPort());
		socket = connectToLeader(addr);
		recvThread = new DataCnxManager.MessageReceiver(this.socket, recvqueue, myId);
		sendThread = new DataCnxManager.MessageSender(this.socket, sendqueue, myId);
		worker = new PINGWorker(myId);
		this.clientAddr = new InetSocketAddress(AvailableConfig.CLIENT_SERVING_IP, AvailableConfig.clientPorts.get(myId));
	}
	
	/**
     * Establish a connection with the Leader found by findLeader. Retries
     * 5 times before giving up. 
     * @param addr - the address of the Leader to connect to.
     * @throws IOException - if the socket connection fails on the 5th attempt
     * @throws ConnectException
     * @throws InterruptedException
     */
    protected Socket connectToLeader(InetSocketAddress addr) 
    throws IOException, ConnectException, InterruptedException {
    	Socket sock = new Socket();        
        sock.setSoTimeout(AvailableConfig.tickTime * AvailableConfig.syncTick);
        for (int tries = 0; tries < 5; tries++) {
            try {
                sock.connect(addr, AvailableConfig.tickTime * AvailableConfig.syncTick);
                sock.setTcpNoDelay(true);
                break;
            } catch (IOException e) {
                if (tries == 4) {
                    LOG.error("Unexpected exception",e);
                    throw e;
                } else {
                    LOG.warn("Unexpected exception, tries="+tries+
                            ", connecting to " + addr,e);
                    sock = new Socket();
                    sock.setSoTimeout(AvailableConfig.tickTime * AvailableConfig.syncTick);
                }
            }
            Thread.sleep(1000);
        }
        return sock;
    }   

	
	
	public void shutdown() {
		LOG.warn("#shut down ping worker");
		recvThread.halt();
		sendThread.halt();
		following = false;
		this.worker.finish();
	}
	
//	public void followLeader() {
//		if (!worker.hasWorked) {
//			worker.start();
//			try {
//				latch.await();
//			} catch (InterruptedException e) {
//				LOG.error("latch await error", e);
//			}
//		}
//		else {
//			LOG.debug("ping worker is still running...");
//		}
//	}	
	
	
	public boolean followLeader() {
		recvThread.start();
		sendThread.start();
		if (!worker.hasWorked) {
			worker.start();
		}
		long time = 0;
		
		ServerCnxnFactory factory = null;
		try {
			factory = ServerCnxnFactory.createFactory();
			factory.configure(this.clientAddr, AvailableConfig.MAX_CLIENT_CNXN);
			factory.setServer(new ApeServer(this));
			factory.start();
		} catch (Exception e) {
			LOG.error("error happened when starting serving for clients, caused:", e);
			if (factory != null)
				factory.shutdown();
			shutdown();
			return false;
		}
		
		
		while(following) {
			try {
				long lastAck = curTick;
				TimeUnit.MILLISECONDS.sleep(AvailableConfig.tickTime);
				this.tick++;
				time++;
				LOG.debug("tick:" + this.tick + ", lastAck:" + this.curTick);
				if (!sync()) {
					shutdown();
					LOG.error("can't sync with leader, tick:" + this.tick + ", curTick:" + this.curTick);
					return false;
				} else {
					LOG.info("sync time:" + time + " following leader[id=" +this.leaderId+ "]");
					//if find any deviation, correct it
					//correct(lastAck);
				}
			} catch (InterruptedException e) {
				shutdown();
				LOG.error("InterruptedException when following leader", e);
				return false;
			}
		}
		shutdown();
		return true;
	}
	
	
	public void stopFollowing() {
		LOG.info("stop following leader gracefully");
		following = false;
	}
	
	public void correct(long lastAck) {
		if (this.tick != curTick && curTick > lastAck)
			curTick = this.tick;
	}
	
	public boolean sync() {
		return worker.isAlive() && curTick > tick - AvailableConfig.syncTick;
	}
	
	class PINGWorker extends Thread {
		boolean hasWorked = false;
		volatile boolean isRunning = false;
		int monitorTime = 0;
		boolean isFirstTime = true;
		int failedCnt = 0;
		int rubbishCnt = 0;
		AtomicInteger i = new AtomicInteger(0);
		public PINGWorker(long id) {
			super("PINGWorker[myid=" + id + "]");
		}
		
		public void run() {
			try {
				hasWorked = true;
				BasicPacket n = null;
				isRunning = true;
				while(isRunning) {
					if (isFirstTime) {
						isFirstTime = false;
						n = new BasicPacket(Type.AGREEMENT, xid, myId, null);
						LOG.info("send AGREEMENT to leader[id=" + leaderId + "]");
						sendqueue.offer(n);
					} else {
						n = new BasicPacket(Type.PING, xid, myId, null);
						LOG.debug("send Not to leader:" + n);
						sendqueue.offer(n);
					}
					n = recvqueue.poll(AvailableConfig.followTimeOut, AvailableConfig.pollTimeUnit);
					if (n == null) {
						//restart
//						failedCnt++;
//						while(failedCnt < AvailableConfig.followFailedCntLimit && n == null) {
//							n = this.election.pollPing(AvailableConfig.followTimeOut, AvailableConfig.pollTimeUnit);
//						}
//						if (failedCnt == AvailableConfig.followFailedCntLimit) {
//							LOG.error("fail polling times:" +failedCnt );
//							followLeaderFailed = true;
//							return;
//						}
//						latch.countDown();
						continue;
					}
					LOG.debug("recv Mes from leader["+leaderId+"]:" + n);
					
					switch (n.getType()) {
					case Type.CONFIRM:
						monitorTime++;
						if (monitorTime == 1) {
							LOG.info("recv leader[id="+n.getFrom()+"] confirmed, cheer!");
						} else {
							LOG.error("there must be something wrong with this code, recv confirm from leader too many times : " + monitorTime);
							followLeaderFailed = true;
							return;
						}
						curTick++;
						break;
					case Type.ACK:
						LOG.debug("recv leader ACK, myid is " + myId + ", xid is " + n.getXid());
						curTick++;
						break;
					case Type.DATA:
						LOG.debug("recv data  myid is " + myId);
						throw new IOException("what data you send!!! Damn it!!, type:" + Type.DATA);
					default:
						rubbishCnt++;
						LOG.debug("recv rubbish msg:" + n + ", rubbish cnt : " + rubbishCnt);
						break;
					}
					
					TimeUnit.MILLISECONDS.sleep(AvailableConfig.tickTime);
				}
			} catch (InterruptedException e) {
				LOG.error("follow leader InterruptedException, reason:" , e);
				shutdown();
			} catch (IOException e) {
				LOG.error(e.getMessage() , e);
				shutdown();
			}
		}
		public void finish() {
			LOG.debug("finish PINGWorker");
			if (!isRunning)
				return;
			isRunning = false;
			this.interrupt();
		}
	}
	
	public class DataProcessor extends Thread {
		volatile boolean isRunning = true;
		
		public void run() {
			while(isRunning) {
				
				
				
			}
			
			
		}
		
		public void halt() {
			if (!isRunning)
				return;
			isRunning = false;
			this.interrupt();
		}
		
		
	}
	
	
	
	@Override
	public boolean isLeader() {
		return false;
	}
	
	@Override
	public long getId() {
		return myId;
	}
}
