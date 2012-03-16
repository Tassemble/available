package com.techq.available.quorum.handler;

import junit.framework.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techq.available.quorum.Election;
import com.techq.available.quorum.ElectionStub;
import com.techq.available.quorum.Notification;
import com.techq.available.quorum.ServerState;
import com.techq.available.quorum.Notification.mType;
import com.techq.available.rubbish.FollowerHandler3;


public class FollowerHandlerTest {
	private static final Logger LOG = LoggerFactory.getLogger(FollowerHandlerTest.class);
	
	@Test
	public void test() {
		Election election = new ElectionStub();
		FollowerHandler3 handler = new FollowerHandler3(election, 2);
		Notification n = new Notification(Notification.mType.CONFIRM,
				1, 0, 1, ServerState.FOLLOWING, 1 , 2);
		try {
			
			boolean isOk = handler.followLeader(n);
			Assert.assertEquals(true, isOk);
			for (int i = 0; i < 10; i++) {
				n = new Notification(Notification.mType.CONFIRM,
						1, 0, 1, ServerState.FOLLOWING, 0 , 2);
				isOk = handler.followLeader(n);
			}
			Assert.assertEquals(true, isOk);
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
