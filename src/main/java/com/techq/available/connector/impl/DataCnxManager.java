package com.techq.available.connector.impl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.jute.BinaryInputArchive;
import org.apache.jute.BinaryOutputArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techq.available.AvailableConfig;
import com.techq.available.data.BasicPacket;
import com.techq.available.quorum.handler.Leader;
import com.techq.available.quorum.handler.LearnerHandler;

/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public class DataCnxManager {
	private static final Logger LOG = LoggerFactory.getLogger(DataCnxManager.class);
	
	class LearnerCnxAcceptor extends Thread {
		private volatile boolean stop = false;

		Leader leader;
		ServerSocket ss;
		long id;
		public LearnerCnxAcceptor(Leader handler, int port, long id) throws IOException {
			super("LearnerCnxAcceptor[myid=" + id + "]");
			this.id = id;
			
			ss = new ServerSocket(port);
			leader = handler;
		}
		
		@Override
		public void run() {
			try {
				while (!stop) {
					try {
						Socket s = ss.accept();
						s.setSoTimeout(AvailableConfig.tickTime * AvailableConfig.syncTick);
						s.setTcpNoDelay(true);
						LearnerHandler fh = new LearnerHandler(s, leader, id);
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
			LOG.debug("halt LearnerCnxAcceptor");
		}
	}
	
	
	public static class MessageReceiver extends Thread {
		volatile boolean running = true;
		Socket sock;
		LinkedBlockingQueue<BasicPacket> recvqueue;
		public MessageReceiver(Socket sock, LinkedBlockingQueue<BasicPacket> re, long id) {
			super("MessageReceiver[myid=" + id + "]");
			this.sock = sock;
			recvqueue = re;
		}
		
		@Override
		public void run() {
			
			BinaryInputArchive ia = null;
			try {
				ia = BinaryInputArchive.getArchive(sock.getInputStream());
			} catch (IOException e1) {
				e1.printStackTrace();
				this.running = false;
				return;
			}
			while(running) {
				try {
//					byte requestBytes[];
//					if (DebugConfig.debug)
//						requestBytes = new byte[Message.DEBUG_DEFAULT_SIZE];
//					else
//						requestBytes = new byte[Message.DEFAULT_SIZE];
					BasicPacket packet = new BasicPacket();
					ia.readRecord(packet, "BasicPacket");
//					in.read(requestBytes);
//					ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);
//					Notification n = new Notification();
//					int type = requestBuffer.getInt();
//					int state = requestBuffer.getInt();
//					long leader = requestBuffer.getLong();
//					long zxid = requestBuffer.getLong();
//					long clock = requestBuffer.getLong();
//					long from = requestBuffer.getLong();
//					int seq = requestBuffer.getInt();
//					n.setType(Notification.getTypeByInt(type));
//					n.setState(Notification.getStateByInt(state));
//					n.setLeader(leader);
//					n.setZxid(zxid);
//					n.setLogicalClock(clock);
//					n.setFrom(from);
////					n.setSid(-1);
//					if (DebugConfig.debug)
//						n.setSeq(seq);
					recvqueue.offer(packet);
				} catch (IOException e) {
					LOG.error("read time", e);
					running = false;
				}
			}
		}
		
		public void halt() {
			if (!running)
				return;
			running = false;
			LOG.debug("halt MessageReceiver");
			this.interrupt();
		}
		
	}
	
	
	public static class MessageSender extends Thread {
		volatile boolean running = true;
		
		
		Socket sock;
		LinkedBlockingQueue<BasicPacket> sendqueue;
		public MessageSender(Socket sock, LinkedBlockingQueue<BasicPacket> se, long id) {
			super("MessageSender[myid=" + id + "]");
			this.sock = sock;
			sendqueue = se;
		}
		
		@Override
		public void run() {
			BinaryOutputArchive oa = null;
			try {
				oa = BinaryOutputArchive.getArchive(sock.getOutputStream());
			} catch (IOException e1) {
				e1.printStackTrace(System.out);
				this.running = false;
				return;
			}
			while(running) {
				try {
					BasicPacket p = sendqueue.poll(200, TimeUnit.MILLISECONDS);
					if (p == null) {
						p = sendqueue.take();
					}
					LOG.trace("send:" + p);
//					byte requestBytes[];
//					if (DebugConfig.debug)
//						requestBytes = new byte[Message.DEBUG_DEFAULT_SIZE];
//					else
//						requestBytes = new byte[Message.DEFAULT_SIZE];
//					ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);
//					requestBuffer.clear();
//					requestBuffer.putInt(m.getType().ordinal());
//					requestBuffer.putInt(m.getState().ordinal());
//					requestBuffer.putLong(m.getLeader());
//					requestBuffer.putLong(m.getZxid());
//					requestBuffer.putLong(m.getLogicalClock());
//					requestBuffer.putLong(m.getFrom());//from which peer
////					requestBuffer.putLong(m.getSid());//from which peer
//					if (DebugConfig.debug)
//						requestBuffer.putInt(m.getSeq());
					oa.writeRecord(p, "BasicPacket");
//					try {
//						dos.write(requestBytes);
//					} catch (IOException e) {
//						LOG.error("write exception", e);
//						this.halt();
//					}
				} catch (InterruptedException e) {
					running = false;
					LOG.error("InterruptedException", e);
				} catch (IOException e) {
					running = false;
					LOG.error("InterruptedException", e);
				}
			}
		}
		public void halt() {
			if (!running)
				return;
			running = false;
			LOG.debug("halt MessageSender");
			this.interrupt();
		}
	}
	
	


}
