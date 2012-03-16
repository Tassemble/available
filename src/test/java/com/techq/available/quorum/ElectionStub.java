package com.techq.available.quorum;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElectionStub implements Election {
	private static final Logger LOG = LoggerFactory.getLogger(ElectionStub.class);
	LinkedBlockingQueue<Notification> sendqueue = new LinkedBlockingQueue<Notification>();
	LinkedBlockingQueue<Notification> recvqueue = new LinkedBlockingQueue<Notification>();
	
	@Override
	public long whoIsLeader() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Vote lookForLeader() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Notification pollConfirm(long timeout, TimeUnit unit)
			throws InterruptedException {
		return recvqueue.poll(timeout, unit);
	}

	@Override
	public void offerACK(Notification n) {
//		try {
//			LOG.info("wait 1 s");
//			TimeUnit.SECONDS.sleep(1);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		recvqueue.offer(n);
		
	}

	@Override
	public void pushback(Notification n) throws InterruptedException {
		recvqueue.offer(n);
	}
	
	@Override
	public Notification pollPing(long timeout, TimeUnit unit)
			throws InterruptedException {
		Notification n = recvqueue.poll(timeout, unit);
		if (n != null) {
			LOG.info("recv type:" + n.getType() + " from sid:" + n.sid);
		}
		return n;
	}

	
	@Override
	public void offerPING(Notification n) throws InterruptedException {
		LOG.info("send type:" + n.getType() + " to sid:" + n.sid);
		
		recvqueue.offer(n);
	}
	
	
	@Override
	public void offerAgree(Notification n) {
		LOG.info("send type:" + n.getType() + " to sid:" + n.sid);
		recvqueue.offer(n);
	}
}
