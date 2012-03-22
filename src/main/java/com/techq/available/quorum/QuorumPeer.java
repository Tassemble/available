package com.techq.available.quorum;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.UnexpectedException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.print.attribute.Size2DSyntax;

import org.omg.CORBA.TIMEOUT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techq.available.App;
import com.techq.available.quorum.handler.Follower;
import com.techq.available.quorum.handler.Leader;


/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public class QuorumPeer extends Thread {
	private static final Logger LOG = LoggerFactory.getLogger(App.class);
	ProposalVote curVote;
	long logicalClock = 0;
	private long id = -1;
	private long zxid = 0;
	public long startTime = 0;
	volatile boolean isGoingOn = true;
	public Map<Long, QuorumServer> quorumPeers;
	public InetSocketAddress electionAddr;
	volatile ServerState state;
	Election election;
	boolean isRunning = true;
	private int initLimit = 5;
	public InetSocketAddress addr;

	

	
	public Election getElection() {
		return election;
	}



	public ProposalVote getCurVote() {
		return curVote;
	}



	public long getLogicalClock() {
		return logicalClock;
	}
	
	
	public void setLogicalClock(long logicalClock) {
		this.logicalClock = logicalClock;
	}
	
	
	public int getInitLimit() {
		return initLimit;
	}
	
	
	
	public void setInitLimit(int initLimit) {
		this.initLimit = initLimit;
	}

	
	
	
	public QuorumPeer(Map quorumPeers, ProposalVote curVote, long id) {
		super("QuorumPeer[myid=" + id + "]");
		this.quorumPeers = quorumPeers;
		this.curVote = curVote;
		this.id = id;
		this.setPeerState(ServerState.LOOKING);
		election = new LeaderElection(this);
	}
	
	

	
	
	public long getTickTime() {
		return tickTime;
	}
	public void setTickTime(long tickTime) {
		this.tickTime = tickTime;
	}
	public long getSyncLimit() {
		return syncLimit;
	}
	public void setSyncLimit(long syncLimit) {
		this.syncLimit = syncLimit;
	}



	private long tickTime = 1000;
	private long syncLimit;
	volatile protected int tick;
	
	

	public long getId() {
		return id;
	}
	public long getZxid() {
		return zxid;
	}
	
	
	public Map<Long, QuorumPeer.QuorumServer> getVotingViews() {
		return Collections.unmodifiableMap(this.quorumPeers);
	}
	
	
	public Map<Long, QuorumPeer.QuorumServer> getViews() {
		return Collections.unmodifiableMap(this.quorumPeers);
	}
	
	
	public static class QuorumServer {
		public QuorumServer(long id, InetSocketAddress addr, InetSocketAddress electionAddr) {
			this.id = id;
			this.addr = addr;
			this.electionAddr = electionAddr;
		}

		public QuorumServer(long id, InetSocketAddress addr) {
			this.id = id;
			this.addr = addr;
			this.electionAddr = null;
		}

		public QuorumServer(long id, InetSocketAddress addr, InetSocketAddress electionAddr,
				LearnerType type) {
			this.id = id;
			this.addr = addr;
			this.electionAddr = electionAddr;
			this.type = type;
		}

		public InetSocketAddress addr;

		public InetSocketAddress electionAddr;

		public long id;

		public LearnerType type = LearnerType.PARTICIPANT;
	}
	
	public enum LearnerType {
		PARTICIPANT, OBSERVER;
	}
	
	
	
	public ServerState getPeerState() {
		return state;
	}
	
	
	public void setPeerState(ServerState newState) {
		state = newState;
	}
	
	
	@Override
	public void run() {
		
		LOG.info("Starting quorum peer");
		
		while (isRunning) {
			switch (getPeerState()) {
			case LOOKING:
				try {
						Vote candidate = election.lookForLeader();
					} catch (Exception e) {
						LOG.warn("Unexpected exception", e);
						setPeerState(ServerState.LOOKING);
					}
					break;
			case FOLLOWING:
				try {
					LOG.info("i am follower, i have nothing to do but following");
					QuorumServer leaderServer = quorumPeers.get(curVote.proposedLeader);
					Follower handler;
					handler = new Follower(curVote.proposedLeader, curVote.proposedZxid, id, leaderServer.addr);
					boolean isOK = handler.followLeader();
					if (!isOK) {
						setPeerState(ServerState.LOOKING);
						LOG.info("can't sync with leader, and restart");
						//TimeUnit.SECONDS.sleep(10);
						setPeerState(ServerState.LOOKING);
					} else {
						LOG.info("follow[id=" +id+"] exit");
						return;
					}
				} catch (InterruptedException e1) {
					LOG.warn("Unexpected InterruptedException", e1);
					setPeerState(ServerState.LOOKING);
				} catch (IOException e) {
					LOG.warn("Unexpected IOException", e);
					setPeerState(ServerState.LOOKING);
				}
				
				break;
			case LEADING:
				LOG.info("i am leader, i have nothing to do but leading");
				Set<Long> views = this.getViews().keySet();
				QuorumServer leaderServer = quorumPeers.get(curVote.proposedLeader);
				try {
					Leader leaderHandler = new Leader(curVote.proposedLeader, views, leaderServer.addr);
				//block here
				
					boolean isOK = leaderHandler.leading();
					if (isOK) {
						LOG.warn("leader[id="+id+"] exit!!!");
						return;
					} else {
						setPeerState(ServerState.LOOKING);
						LOG.error("can't sync with followers, and restart");
						//TimeUnit.SECONDS.sleep(10);
					}
				} catch (InterruptedException e1) {
					LOG.error("can't sync with followers, and restart", e1);
					setPeerState(ServerState.LOOKING);
				} catch (IOException e) {
					LOG.error("can't sync with followers, and restart", e);
					setPeerState(ServerState.LOOKING);
				} catch (Exception e) {
					LOG.error("unexpected happened", e);
					setPeerState(ServerState.LOOKING);
				}
				
				break;
			}
		}
	}	
	
	public boolean synced() {
		return this.tick <= this.syncLimit;
	}
	
	
	
	
}
