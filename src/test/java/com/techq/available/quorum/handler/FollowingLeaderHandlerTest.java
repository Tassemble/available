package com.techq.available.quorum.handler;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techq.available.quorum.Election;
import com.techq.available.quorum.ElectionStub;
import com.techq.available.quorum.FollowerElectionStub;
import com.techq.available.quorum.handler.Follower;

public class FollowingLeaderHandlerTest {

	private static final Logger LOG = LoggerFactory.getLogger(FollowerHandlerTest.class);
	
	@Test
	public void test() throws InterruptedException, IOException {
		Election election = new FollowerElectionStub();
		((FollowerElectionStub)election).init();
		Follower handler = new Follower(2, 0, 1, null);
		handler.followLeader();
	}
	
	
}
