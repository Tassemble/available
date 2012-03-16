package com.techq.available.quorum.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.print.attribute.standard.Finishings;

import org.apache.jute.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.server.Type;
import com.techq.available.AvailableConfig;
import com.techq.available.connector.impl.DataCnxManager;
import com.techq.available.data.BasicPacket;
import com.techq.available.quorum.DebugConfig;
import com.techq.available.quorum.Election;
import com.techq.available.quorum.Message;
import com.techq.available.quorum.Notification;


/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public class LearnerHandler extends Thread  {
	private static final Logger LOG = LoggerFactory.getLogger(LearnerHandler.class);
	LinkedBlockingQueue<BasicPacket> sendqueue = new LinkedBlockingQueue<BasicPacket>();
	LinkedBlockingQueue<BasicPacket> recvqueue = new LinkedBlockingQueue<BasicPacket>();

	
	final Socket sock;
	Leader leader;
	volatile long lastTick;
	volatile long preAck;
	DataCnxManager.MessageReceiver recvThread;
	DataCnxManager.MessageSender sendThread;
	ProcessorWorker worker;
	
	
	
	public LearnerHandler(Socket sock, Leader leader, long id) throws IOException {
		super("LearnerHandler-" + sock.getRemoteSocketAddress());
		this.sock = sock;
		lastTick = leader.tick;
		this.leader = leader;
		this.leader.addLearnerHandler(this);
		recvThread = new DataCnxManager.MessageReceiver(this.sock, recvqueue, id);
		sendThread = new DataCnxManager.MessageSender(this.sock, sendqueue, id);
		worker = new ProcessorWorker(id);
	}
	
	public synchronized void shutdown() {
		try {
			if (recvThread.isAlive())
				recvThread.halt();
			if (sendThread.isAlive())
				sendThread.halt();
			if (worker.isAlive())
				worker.finish();
			if (this.sock != null && this.sock.isConnected()) {
				this.sock.close();
			}
			if (this.isAlive())
				this.interrupt();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(System.out);
		}
		leader.removeLearnerHandler(this);
		
	}
	
	@Override
	public void run() {
		worker.start();
		recvThread.start();
		sendThread.start();
		try {
			//wait for them
			worker.join();
			recvThread.join();
			sendThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(System.out);
			shutdown();
		}
	} 
	
	public boolean sync(long leadTick) {
		boolean res = this.isAlive() && lastTick > leadTick - AvailableConfig.syncTick;
		if (res == true)
			LOG.debug("sync ok, last tick:" + lastTick + ", tick:" + leadTick);
		return res;
	}
	
	
	public BasicPacket pollRecvQueue(long time, TimeUnit unit) throws InterruptedException
	{
		return recvqueue.poll(time, unit);
	}
	
	public void toSend(BasicPacket n)
	{
		sendqueue.offer(n);
	}
	
	public void correct(long tick) {
		if (tick != lastTick && lastTick > preAck)
			lastTick = tick;
	}
	
	
	
	/*class MessageReceiver extends Thread {
		volatile boolean running = true;
		
		@Override
		public void run() {
			
			DataInputStream in = null;
			try {
				in = new DataInputStream(sock.getInputStream());
			} catch (IOException e1) {
				e1.printStackTrace();
				this.halt();
				return;
			}
			while(running) {
				try {
					byte requestBytes[];
					if (DebugConfig.debug)
						requestBytes = new byte[Message.DEBUG_DEFAULT_SIZE];
					else
						requestBytes = new byte[Message.DEFAULT_SIZE];
					in.read(requestBytes);
					ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);
					Notification n = new Notification();
					int type = requestBuffer.getInt();
					int state = requestBuffer.getInt();
					long leader = requestBuffer.getLong();
					long zxid = requestBuffer.getLong();
					long clock = requestBuffer.getLong();
					long from = requestBuffer.getLong();
					long sid = requestBuffer.getLong();
					n.setType(Notification.getTypeByInt(type));
					n.setState(Notification.getStateByInt(state));
					n.setLeader(leader);
					n.setZxid(zxid);
					n.setLogicalClock(clock);
					n.setFrom(from);
					n.setSid(sid);
					recvqueue.offer(n);
				} catch (IOException e) {
					LOG.error("read time", e);
					this.halt();
					shutdown();
				}
			}
		}
		
		public void halt() {
			if (!running)
				return;
			running = false;
			this.interrupt();
		}
		
	}
	
	
	class MessageSender extends Thread {
		volatile boolean running = true;
		@Override
		public void run() {
			DataOutputStream dos = null;
			try {
				dos = new DataOutputStream(sock.getOutputStream());
			} catch (IOException e1) {
				e1.printStackTrace(System.out);
				this.halt();
				shutdown();
				return;
				
			}
			while(running) {
				try {
					Notification m = sendqueue.poll(200, TimeUnit.MILLISECONDS);
					if (m == null)
						continue;
					LOG.info("send:" + m);
					byte requestBytes[];
					if (DebugConfig.debug)
						requestBytes = new byte[Message.DEBUG_DEFAULT_SIZE];
					else
						requestBytes = new byte[Message.DEFAULT_SIZE];
					
					ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);
					requestBuffer.clear();
					requestBuffer.putInt(m.getType().ordinal());
					requestBuffer.putInt(m.getState().ordinal());
					requestBuffer.putLong(m.getLeader());
					requestBuffer.putLong(m.getZxid());
					requestBuffer.putLong(m.getLogicalClock());
					requestBuffer.putLong(m.getFrom());//from which peer
					requestBuffer.putLong(m.getSid());//from which peer
					if (DebugConfig.debug)
						requestBuffer.putInt(m.getSeq());
					try {
						dos.write(requestBytes);
					} catch (IOException e) {
						LOG.error("write exception", e);
						this.halt();
						shutdown();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		public void halt() {
			if (!running)
				return;
			running = false;
			this.interrupt();
		}
	}*/
	
	
	class ProcessorWorker extends Thread {
		volatile boolean running = true;

		ProcessorWorker(long id) {
			super("ProcessorWorker[myid=" + id + "]");
		}
		
		public void run() {
			while(running) {
				BasicPacket n = null;
				try {
					n = recvqueue.poll(AvailableConfig.pollTimeout, AvailableConfig.pollTimeUnit);
				} catch (InterruptedException e) {
					LOG.error("this should not be happened when responsing to follower", e);
				}
				if (n == null || n != null && !leader.followers.contains(n.getFrom())) {
					if (n != null) { 
						LOG.warn("rubbish message :" + n);
					}
					continue;
				}
				LOG.trace("recv msg:" + n);
				switch(n.getType()) {
				case Type.PING:
					LOG.debug("update sid:" + n.getFrom() + ", now its tick:" + lastTick);
					preAck = lastTick;
					lastTick = leader.tick;
					n.setType(Type.ACK);
					n.setFrom(leader.leaderId);
					LOG.debug("send msg:" + n);
					sendqueue.offer(n);
					break;
				case Type.AGREEMENT:
					synchronized (leader.quorums) {
						leader.quorums.add(n.getFrom());
					}
					n.setType(Type.CONFIRM);
					n.setFrom(leader.leaderId);
					LOG.debug("send msg:" + n);
					sendqueue.offer(n);
					break;
				case Type.DATA:
					lastTick = leader.tick;
					LOG.debug("recv type:DATA myid:" + leader.leaderId);
					leader.addPackets(n);
					break;
				default:
					LOG.warn("rubbish message :" + n);
					break;
				}
			}
		}
		
		public void finish() {
			if (!running)
				return;
			running = false;
			LOG.debug("halt ProcessorWorker");
			this.interrupt();
		}
		
	}
	
	
	
}
