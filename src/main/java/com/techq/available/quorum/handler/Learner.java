package com.techq.available.quorum.handler;


import com.techq.available.data.BasicPacket;

public interface Learner {

	public long getId();
	boolean isLeader();
	void addPackets(BasicPacket packet);
}
