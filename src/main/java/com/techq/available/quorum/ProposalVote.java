package com.techq.available.quorum;

/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public class ProposalVote {
	volatile long logicalclock; /* Election instance */
	volatile long proposedLeader;
	volatile long proposedZxid;
	volatile long proposedEpoch;
	volatile ServerState state;
	
	public ProposalVote(long logicalclock,
			long proposedLeader,
			long proposedZxid,
			ServerState state
	) {
		this.logicalclock = logicalclock;
		this.proposedLeader =  proposedLeader;
		this.proposedZxid = proposedZxid;
		this.state = state;
	}

	
	
	@Override
	public String toString() {
		return "ProposalVote [logicalclock=" + logicalclock + ", proposedLeader=" + proposedLeader
				+ ", proposedZxid=" + proposedZxid + ", proposedEpoch=" + proposedEpoch
				+ ", state=" + state + "]";
	}



	public long getLogicalclock() {
		return logicalclock;
	}



	public void setLogicalclock(long logicalclock) {
		this.logicalclock = logicalclock;
	}



	public long getProposedLeader() {
		return proposedLeader;
	}



	public void setProposedLeader(long proposedLeader) {
		this.proposedLeader = proposedLeader;
	}



	public long getProposedZxid() {
		return proposedZxid;
	}



	public void setProposedZxid(long proposedZxid) {
		this.proposedZxid = proposedZxid;
	}



	public long getProposedEpoch() {
		return proposedEpoch;
	}



	public void setProposedEpoch(long proposedEpoch) {
		this.proposedEpoch = proposedEpoch;
	}



	public ServerState getState() {
		return state;
	}



	public void setState(ServerState state) {
		this.state = state;
	}
	
	
	
	
}
