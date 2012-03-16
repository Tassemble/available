package com.techq.available.quorum;

/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public class Vote {

	final private long id;

	final private long zxid;

	final private long electionEpoch;

	final private ServerState state;

	public Vote(long id, long zxid, long electionEpoch, ServerState state) {
		this.id = id;
		this.zxid = zxid;
		this.electionEpoch = electionEpoch;
		this.state = state;
	}

	public Vote(ProposalVote curVote) {
		this.id = curVote.proposedLeader;
		this.zxid = curVote.proposedZxid;
		this.electionEpoch = curVote.logicalclock;
		this.state = curVote.state;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Vote)) {
			return false;
		}
		Vote other = (Vote) o;
		return (id == other.id && zxid == other.zxid && electionEpoch == other.electionEpoch);

	}

	@Override
	public int hashCode() {
		return (int) (id & zxid);
	}

	public String toString() {
		return "(" + id + ", " + Long.toHexString(zxid) + ")";
	}

	public long getId() {
		return id;
	}

	public long getZxid() {
		return zxid;
	}

	public long getElectionEpoch() {
		return electionEpoch;
	}

	public ServerState getState() {
		return state;
	}
}
