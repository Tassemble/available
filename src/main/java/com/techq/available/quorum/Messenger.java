package com.techq.available.quorum;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techq.available.connector.ElectionCnxManager;

/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public class Messenger {
	static Logger LOG = LoggerFactory.getLogger(Messenger.class);
	QuorumPeer curPeer;
	LinkedBlockingQueue<Notification> sendqueue;
	LinkedBlockingQueue<Notification> recvqueue;
	LinkedBlockingQueue<Notification> pingQueue;
	ProposalVote curVote;
	WorkerSender ws;
	ElectionCnxManager manager;
	WorkerReceiver wr;
	
	
	public Messenger(
			ElectionCnxManager manager,
			QuorumPeer peer, 
			LinkedBlockingQueue<Notification> sendqueue,
			LinkedBlockingQueue<Notification> recvqueue,
			LinkedBlockingQueue<Notification> agreequeue,
			ProposalVote curVote) {
		this.manager = manager;
		curPeer = peer;
		this.sendqueue = sendqueue;
		this.recvqueue = recvqueue;
		this.pingQueue = agreequeue;
		this.curVote = curVote;
	}

	
	public void start() {
		this.ws = new WorkerSender(manager);
		Thread t = new Thread(this.ws, "WorkerSender[myid=" + curPeer.getId() + "]");
		t.setDaemon(true);
		t.start();

		this.wr = new WorkerReceiver(manager);

		t = new Thread(this.wr, "WorkerReceiver[myid=" + curPeer.getId() + "]");
		t.setDaemon(true);
		t.start();		
	}
	
	void halt() {
		this.ws.stop = true;
		this.wr.stop = true;
	}

	/**
	 * Receives messages from instance of QuorumCnxManager on method run(),
	 * and processes such messages.
	 */
	class WorkerReceiver implements Runnable {
		volatile boolean stop;
		ElectionCnxManager manager;

		WorkerReceiver(ElectionCnxManager manager) {
			this.stop = false;
			this.manager = manager;

		}

		public void run() {

			Message response;
			while (!stop) {
				// Sleeps on receive
				try {
					response = manager.pollRecvQueue(3000, TimeUnit.MILLISECONDS);
					if (response == null)
						continue;

					/**
					 * make sure it is the participant we have known
					 */
					if (curPeer.getVotingViews().containsKey(response.sid)) {

						if (DebugConfig.debug) {
							LOG.debug("#now it is debug mode");
							if (response.buffer.capacity() != Message.DEBUG_DEFAULT_SIZE) {
								LOG.error("Got a short response: " + response.buffer.capacity() + ", expect size:" + Message.DEBUG_DEFAULT_SIZE);
								continue;
							}
						} else {
							if (response.buffer.capacity() != Message.DEFAULT_SIZE) {
								LOG.error("Got a short response: " + response.buffer.capacity() + ", expect size:" + Message.DEFAULT_SIZE);
								continue;
							}
						}
						
						response.buffer.clear();
						
						Notification.mType type = Notification.mType.UNKNOW;
						int val = response.buffer.getInt();
						type = Notification.getTypeByInt(val);
						
						// State of peer that sent this message
						ServerState ackstate = ServerState.LOOKING;
						val = response.buffer.getInt();
						switch (val) {
						case 0:
							ackstate = ServerState.LOOKING;
							break;
						case 1:
							ackstate = ServerState.FOLLOWING;
							break;
						case 2:
							ackstate = ServerState.LEADING;
							break;
						default:
							LOG.error("what's this? value:" + val);
							break;
						}

						// Instantiate Notification and set its attributes

						long leader = response.buffer.getLong();
						long zxid = response.buffer.getLong();
						long electionEpoch = response.buffer.getLong();
						long from = response.buffer.getLong();
						ServerState state = ackstate;
						
						long sid = response.sid;
						
						Notification n = new Notification(
								type,
								leader,
								zxid,
								electionEpoch,
								state,
								from,//to whom
								sid//
								);
						
						
						/*
						 * Print notification info
						 */
						if (DebugConfig.debug) {
							int seq = response.buffer.getInt();
							n.setSeq(seq);
						}
						
						
						if (LOG.isInfoEnabled()) {
							LOG.info("recv new message: myid=" + curPeer.getId() + ", remote peer id:"  + n.sid + ", msg:"+ n.toString());
						}

						/*
						 * If this server is looking, then send proposed leader
						 */
//						if (type.equals(Notification.mType.AGREEMENT) || type.equals(Notification.mType.PING) 
//								|| type.equals(Notification.mType.ACK) || type.equals(Notification.mType.CONFIRM)) {
//							LOG.debug("recv msg, and add msg to ping queue:" + n);
//							pingQueue.offer(n);
//							continue;
//						}
						
						
						/*
						 * current peer is looking, then do it
						 */
						if (curPeer.getPeerState() == ServerState.LOOKING) {
							LOG.trace("===============curPeer.getPeerState() is "
									+ curPeer.getPeerState() + "============");
							recvqueue.offer(n);
						} else {// if current peer is not looking, must be following or leading
							/*
							 * If this server is not looking, but the one that
							 * sent the ack is looking, then send back what it
							 * believes to be the leader.
							 */
							curVote = curPeer.curVote;
							if (ackstate == ServerState.LOOKING && type != Notification.mType.ACCEPT) {
								Notification not = new Notification(
										Notification.mType.PROPOSE,
										curVote.proposedLeader,
										curVote.proposedZxid,
										curVote.logicalclock,
										curPeer.getPeerState(),
										n.sid,
										curPeer.getId()
								);
								
								LOG.info("response to peer[id="+n.getFrom()+"]:" + not);
								sendqueue.offer(not);
							} else {
								if (LOG.isDebugEnabled()) {
									LOG.debug("don't send me that message: My id =  "
										+ curPeer.getId() + ", " + n.toString());
								}
							}
						}
					} else {
						LOG.warn("where is it from ? i am " + curPeer.getId() + " its id is " + response.sid + ".");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					
				}
				
			}
			LOG.info("WorkerReceiver is down");
		}
	}
	
	/**
	 * This worker simply dequeues a message to send and and queues it on
	 * the manager's queue.
	 */

	class WorkerSender implements Runnable {
		volatile boolean stop;
		ElectionCnxManager manager;

		WorkerSender(ElectionCnxManager manager) {
			this.stop = false;
			this.manager = manager;
		}

		public void run() {
			while (!stop) {
				try {
					Notification m = sendqueue.poll(3000, TimeUnit.MILLISECONDS);
					if (m == null)
						continue;
					DebugConfig.msgCnt.addAndGet(1);
					m.setSeq(DebugConfig.msgCnt.get());
					
					LOG.info("send new message: myid = " + curPeer.getId() + ", msg:" + m);
					process(m);
				} catch (InterruptedException e) {
					break;
				}
			}
			LOG.info("WorkerSender is down");
		}

		/**
		 * Called by run() once there is a new message to send.
		 * 
		 * @param m
		 *            message to send
		 */
		private void process(Notification m) {
			manager.toSend(m.sid,  m.toBuffer());
		}
	}
	
	
	
}
