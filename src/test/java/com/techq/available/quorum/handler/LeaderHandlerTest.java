package com.techq.available.quorum.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.Test;

import com.techq.available.quorum.Election;
import com.techq.available.quorum.ElectionStub;
import com.techq.available.quorum.FollowerElectionStub;
import com.techq.available.quorum.LeaderElectionStub;
import com.techq.available.quorum.Notification;
import com.techq.available.quorum.ServerState;

public class LeaderHandlerTest {

	@Test
	public void test() throws Exception {
		Election election = new LeaderElectionStub();
		((LeaderElectionStub)election).init();
		
		Set<Long> followers = new HashSet<Long>();
		followers.add(Long.valueOf(1));
		followers.add(Long.valueOf(2));
		followers.add(Long.valueOf(3));
		InetSocketAddress addr = new InetSocketAddress(1111);
		final Leader handler = new Leader(2, followers, addr);
		
		try {
			new Thread() {
				public void run() {
					boolean isOk = false;
					try {
						isOk = handler.leading();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Assert.assertEquals(true, isOk);
				}
			}.start();
			
			Notification n = new Notification(Notification.mType.AGREEMENT,
					2, 0, 1, ServerState.FOLLOWING, 1 , 1);
			election.offerPING(n);
			n = new Notification(Notification.mType.AGREEMENT,
					2, 0, 1, ServerState.FOLLOWING, 3 , 3);
			election.offerPING(n);
			
			for (int i = 0; ; i++) {
				TimeUnit.SECONDS.sleep(1);
				n = new Notification(Notification.mType.PING,
						2, 0, 1, ServerState.FOLLOWING, 1 , 1);
				election.offerPING(n);
				n = new Notification(Notification.mType.PING,
						2, 0, 1, ServerState.FOLLOWING, 3 , 3);
				election.offerPING(n);
			}			
//			TimeUnit.SECONDS.sleep(5);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
