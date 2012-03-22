package com.forest.ape.mq.impl;

import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

/**
 * create leader sender and 2 followers, when leader send msg, all the followers received.
 * @author CHQ
 * 2012-3-16
 */
public class WorkerFactoryTest {

	@Ignore
	@Test
	public void test() throws InterruptedException {
		WorkerFactory leaderFactory = new WorkerFactory().buildByLeader("2", Arrays.asList("1", "3"), "521");
		leaderFactory.start();
		
		WorkerFactory followerFactory = new WorkerFactory().buildByFollower("3", "521", new DefaultRecvCallableHandler());
		followerFactory.start();
		
		
		leaderFactory.enQueue(new MQPacket("hahah".getBytes(), null));
		Thread.currentThread().join();
	}
	
	
}
