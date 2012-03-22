package com.techq.available.quorum.together;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techq.available.AvailableConfig;
import com.techq.available.quorum.Election;
import com.techq.available.quorum.ElectionStub;
import com.techq.available.quorum.FollowerElectionStub;
import com.techq.available.quorum.handler.Follower;

public class FollowerTest {

	@Ignore
	@Test
	public void test() throws InterruptedException, IOException {
		Election election = new TogetherElectionStub();
		((TogetherElectionStub)election).init(1);
		InetSocketAddress addr = new InetSocketAddress(1111);
		Follower handler = new Follower(2, 0, 1, addr);
		handler.followLeader();
	}
	
	@Ignore
	@Test
	public void test1() throws InterruptedException, IOException {
		InetSocketAddress addr = new InetSocketAddress(1111);
		Follower handler = new Follower(2, 0, 1, addr);
		handler.followLeader();
	}
	
	@Ignore
	@Test
	public void test2() throws InterruptedException, IOException {
		InetSocketAddress addr = new InetSocketAddress(1111);
		Follower handler = new Follower(2, 0, 3, addr);
		handler.followLeader();
	}
	
	
}
