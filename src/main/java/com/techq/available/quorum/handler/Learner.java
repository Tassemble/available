package com.techq.available.quorum.handler;

import java.nio.ByteBuffer;

import com.techq.available.data.BasicPacket;

public interface Learner {

	public long getId();
	boolean isLeader();
	void addPackets(BasicPacket packet);
}
