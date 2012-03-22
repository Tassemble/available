package com.techq.available.quorum.together;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techq.available.quorum.Election;
import com.techq.available.quorum.LeaderElectionStub;
import com.techq.available.quorum.Notification;
import com.techq.available.quorum.ServerState;
import com.techq.available.quorum.handler.Leader;
import com.techq.available.quorum.handler.LearnerHandler;

public class LeaderTest {
	private static final Logger LOG = LoggerFactory.getLogger(LeaderTest.class);
	@Ignore
	@Test
	public void test() throws Exception {
		Election election = new TogetherElectionStub();
		//server
		((TogetherElectionStub)election).init(0);
		
		Set<Long> followers = new HashSet<Long>();
		followers.add(Long.valueOf(1));
		followers.add(Long.valueOf(2));
		followers.add(Long.valueOf(3));
		InetSocketAddress addr = new InetSocketAddress(1111);
		final Leader handler = new Leader(2, followers, addr);
		try {
			handler.leading();
		} catch (IOException e) {
			LOG.error("IOException", e);
		} catch (InterruptedException e) {
			LOG.error("InterruptedException", e);
		}
	}
	
	@Ignore
	@Test
	public void test1() throws Exception {
		Set<Long> followers = new HashSet<Long>();
		followers.add(Long.valueOf(1));
		followers.add(Long.valueOf(2));
		followers.add(Long.valueOf(3));
		InetSocketAddress addr = new InetSocketAddress(1111);
		final Leader handler = new Leader(2, followers, addr);
		try {
			handler.leading();
		} catch (IOException e) {
			LOG.error("IOException", e);
		} catch (InterruptedException e) {
			LOG.error("InterruptedException", e);
		}
	}
}
