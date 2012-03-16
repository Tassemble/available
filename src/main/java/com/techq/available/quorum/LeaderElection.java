package com.techq.available.quorum;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techq.available.connector.ElectionCnxManager;
import com.techq.available.connector.impl.ElectionCnxManagerImpl;

/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public class LeaderElection implements Election {
	static Logger LOG = LoggerFactory.getLogger(LeaderElection.class);
	AtomicInteger msgCount = new AtomicInteger(0);
	LinkedBlockingQueue<Notification> sendqueue = new LinkedBlockingQueue<Notification>();
	LinkedBlockingQueue<Notification> recvqueue = new LinkedBlockingQueue<Notification>();
	LinkedBlockingQueue<Notification> pingQueue = new LinkedBlockingQueue<Notification>();

	HashMap<Long, Vote> recvVotes = new HashMap<Long, Vote>();
	HashMap<Long, Vote> outOfVotes = new HashMap<Long, Vote>();
	QuorumPeer self;
	boolean isRunning = true;
	boolean isAlreadyInited = false;
	ElectionCnxManager manager = null;
	
	public LeaderElection(QuorumPeer peer) {
		self = peer;
		manager = new ElectionCnxManagerImpl(peer);
	}
	
	public void startListen() {
		if (manager != null)
			manager.startListen();
		else 
			LOG.error("i can't bind to the port for ElectionCnxManager is null");
	}
	
	public void halt() {
		
	}

	
	@Override
	public Vote lookForLeader() throws InterruptedException {
		
		LOG.info("start looking for leader");
		int notTimeout = 200;
		int finalizeWait = 200;
		int maxNotificationInterval = 60000;
		self.startTime = System.currentTimeMillis();
		self.logicalClock++;
		if (!isAlreadyInited) {
			isAlreadyInited = true;
			startListen();
			Messenger messenger = new Messenger(manager, self, sendqueue, recvqueue, pingQueue, self.curVote);
			messenger.start();
		}
		
		resetPeer();
		/**
		 * start propse
		 */
		sendNotifications(Notification.mType.PROPOSE, this.self.getId());

		/**
		 * start running
		 */
		while (self.getPeerState() == ServerState.LOOKING && isRunning()) {
			Notification n = recvqueue.poll(notTimeout, TimeUnit.MILLISECONDS);
			if (n == null) {
				LOG.debug("======================Get nothing from poll===================");
				if (manager.haveDelivered()) {
					sendNotifications(Notification.mType.PROPOSE, this.self.getId());
				} else {
					LOG.debug("manager: connected all");
					manager.connectAll();
				}
				/*
				 * Exponential backoff
				 */
				int tmpTimeOut = notTimeout * 2;
				notTimeout = (tmpTimeOut < maxNotificationInterval ? tmpTimeOut
						: maxNotificationInterval);
				LOG.info("Notification time out: " + notTimeout);
			} else if (self.getVotingViews().containsKey(n.sid)) {
				switch (n.state) {
				case LOOKING:
					if (n.logicalClock > self.logicalClock) {
						self.logicalClock = n.logicalClock;
						self.curVote.logicalclock = n.logicalClock;
						recvVotes.clear();
						if (shouldUpdate(new Vote(n.sid, n.zxid, n.logicalClock, n.state))) {
							updateProposal(n.leader, n.zxid);
						} else {
							//clear all the votes and reset my proposed vote
							updateProposal(self.getId(), self.getZxid());
						}
						sendNotifications(Notification.mType.ACCEPT, this.self.getId());
					} else if (n.logicalClock < self.logicalClock) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("Notification election epoch is smaller than logicalclock. n.electionEpoch = "
									+ n.logicalClock
									+ ", Logical clock"
									+ self.curVote.logicalclock);
						}
						//directly response to it
						long toId = n.from;
						//let remote peer know mine
						Notification not = new Notification(
								Notification.mType.DISAGREE,
								self.curVote.proposedLeader,
								self.curVote.proposedZxid,
								self.logicalClock,
								self.getPeerState(),
								toId, //msg to this guy
								self.getId()// from me
						);
						sendqueue.offer(not);
						//do not put this vote
						break;
					} else if (shouldUpdate(new Vote(n.sid, n.zxid, n.logicalClock,n.state))) {
						updateProposal(n.leader, n.zxid);
						sendNotifications(Notification.mType.ACCEPT, this.self.getId());
					}

					if (LOG.isDebugEnabled()) {
						LOG.debug("Adding vote: From = " + n.sid + ", Proposed leader = "
								+ n.leader + ", Proposed zxid = " + n.zxid
								+ ", Proposed election epoch = " + n.logicalClock);
					}

					Vote v = new Vote(n.leader, n.zxid, n.logicalClock,n.state);
					
					recvVotes.put(n.sid, v);
					//waitAmoment(n);
					// check if it has quorum
					if (containsQuorum(recvVotes, v)) {
						// wait a moment and see if any changed
						while ((n = recvqueue.poll(finalizeWait, TimeUnit.MILLISECONDS)) != null) {
							if (shouldUpdate(new Vote(n.leader, n.zxid, n.logicalClock,n.state))) {
								recvqueue.put(n);
								break;
							}
						}

						if (n == null) {
							self.setPeerState((v.getId() == self.getId()) ? ServerState.LEADING
									: ServerState.FOLLOWING);
							self.curVote.state = self.getPeerState();
							self.curVote.proposedLeader = v.getId();
							Vote endVote = new Vote(self.curVote);
							leaveInstance(endVote);
							return endVote;
						}
					}
					break;
				case FOLLOWING:
				case LEADING:
					LOG.trace("============Received notification, n.electionEpoch is "
							+ n.logicalClock + ", current logicalclock is "
							+ self.curVote.logicalclock + "========================");

					// is same election epoch?
					if (isSameEpoch(n.logicalClock)) {
						recvVotes.put(n.sid, new Vote(n.leader, n.zxid, n.logicalClock,
								 n.state));

						if (shouldUpdate(new Vote(n.leader, n.zxid, n.logicalClock,
								n.state))) {
							updateProposal(n.leader, n.zxid);
							sendNotifications(Notification.mType.ACCEPT, this.self.getId());
							// avoid the instant livelock
							if (checkLeader(recvVotes, n.leader, n.logicalClock)) {
								self.setPeerState((n.leader == self.getId()) ? ServerState.LEADING
										: ServerState.FOLLOWING);
								LOG.debug("=====================recvset, leader is " + n.leader
										+ "=======================");
								Vote endVote = new Vote(n.leader, n.zxid, n.logicalClock,
										n.state);

								leaveInstance(endVote);
								return endVote;
							}
						}
					}
					Vote vote = new Vote(n.leader, n.zxid, n.logicalClock, n.state);
					
					outOfVotes.put(n.sid, vote);
					LOG.info("add to out of votes:" + n + ", now size is:" + outOfVotes.size());
					if (containsQuorum(outOfVotes, vote) && checkLeader(outOfVotes, n.leader, n.logicalClock)) {
						self.logicalClock = n.logicalClock;
						self.curVote.proposedLeader = n.leader;
						self.setPeerState((n.leader == self.getId()) ? ServerState.LEADING
								: ServerState.FOLLOWING);
						self.curVote.state = self.getPeerState();
						LOG.info("=====================recvset, leader is " + n.leader
								+ "=======================");
						Vote endVote = new Vote(n.leader, n.zxid, n.logicalClock,
								n.state);
						leaveInstance(endVote);
						return endVote;
					}
					break;

				default:
					LOG.warn("Notification state unrecoginized: " + n.state + " (n.state), "
							+ n.sid + " (n.sid)");
					break;
				}

			} else {
				LOG.warn("Ignoring notification from non-cluster member " + n.sid);
			}
		}
		return null;
	}

	private void resetPeer() {
		updateProposal(self.getId(), self.getZxid());
		self.curVote = new ProposalVote(self.logicalClock, self.getId(), self.getZxid(), ServerState.LOOKING);
	}

	private void waitAmoment(Notification n) {
		if (n != null)
			msgCount.addAndGet(1);
		try {
			LOG.info("wait a moment, current votes is "+recvVotes.size()+", collect msg:" + msgCount.get() + ", msg:" + n);
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	private void waitAmoment() {
		try {
			LOG.info("wait a moment");
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private boolean isSameEpoch(long electionEpoch) {
		return self.getLogicalClock() == electionEpoch;
	}

	private boolean checkLeader(HashMap<Long, Vote> votes, long leader, long electionEpoch) {

		boolean predicate = true;

		/*
		 * If everyone else thinks I'm the leader, I must be the leader. The
		 * other two checks are just for the case in which I'm not the leader.
		 * If I'm not the leader and I haven't received a message from leader
		 * stating that it is leading, then predicate is false.
		 */

		if (leader != self.getId()) {
			if (votes.get(leader) == null) {
				predicate = false;
			} else if (votes.get(leader).getState() != ServerState.LEADING) {
				predicate = false;
			}
		}

		return predicate;
	}

	private void leaveInstance(Vote v) {
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("About to leave FLE instance: Leader= " + v.getId() + ", Zxid = "
					+ v.getZxid() + ", My id = " + self.getId() + ", My state = "
					+ self.getPeerState());
		}
		recvqueue.clear();
		recvVotes.clear();
		outOfVotes.clear();
	}

	
	/**
	 * 
	 * @param votes
	 * @return
	 * boolean
	 */
	boolean containsQuorum(HashMap<Long, Vote> votes, Vote vote) {
		HashSet<Long> set = new HashSet<Long>();

		/*
		 * First make the views consistent. Sometimes peers will have different
		 * zxids for a server depending on timing.
		 */
 		for (Map.Entry<Long, Vote> entry : votes.entrySet()) {
			if (vote.equals(entry.getValue())) {
				set.add(entry.getKey());
			}
		}
		return (set.size() * 2) > self.getVotingViews().size();
	}

	private boolean isRunning() {
		return isRunning;
	}

	private void updateProposal(long id, long zxid) {
		LOG.info("update proposal leader to id:" + id);
		self.curVote.proposedLeader = id;// self.getId();
		self.curVote.proposedZxid = zxid;// self.getZxid();
	}

	/**
	 * check if succeeds our current vote
	 * 
	 * @param newVote
	 * @return boolean
	 */
	private boolean shouldUpdate(Vote newVote) {
		long remoteZxid = newVote.getZxid();
		long remoteId = newVote.getId();

		return (((remoteZxid > self.getZxid())) || ((remoteZxid == self
				.getZxid()) && (remoteId > self.curVote.proposedLeader)));
	}

	private void sendNotifications(Notification.mType type, long from) {
		for (QuorumPeer.QuorumServer server : self.getVotingViews().values()) {
			long sid = server.id;

			Notification notmsg = new Notification(type, self.curVote.proposedLeader, self.curVote.proposedZxid,
					self.logicalClock, self.curVote.state, sid, from);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Sending Notification: " + notmsg);
			}
			sendqueue.offer(notmsg);
		}
	}

	@Override
	public long whoIsLeader() {
		return -1;
	}
	

	@Override
	public Notification pollConfirm(long timeout, TimeUnit unit) throws InterruptedException {
		return pingQueue.poll(timeout, unit);
	}

	@Override
	public void offerACK(Notification n) {
		sendqueue.offer(n);
	}
	
	
	@Override
	public void pushback(Notification n) throws InterruptedException {
		pingQueue.offer(n);
	}

	@Override
	public Notification pollPing(long timeout, TimeUnit unit)
			throws InterruptedException {
		return pingQueue.poll(timeout, unit);
	}

	
	@Override
	public void offerPING(Notification n) throws InterruptedException {
		LOG.info("send type:" + n.getType() + " to sid:" + n.sid);
		sendqueue.offer(n);
	}

	@Override
	public void offerAgree(Notification n) {
		LOG.info("send type:" + n.getType() + " to sid:" + n.sid);
		sendqueue.offer(n);
	}
	
	
}
