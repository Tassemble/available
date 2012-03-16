package com.techq.available.rubbish;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techq.available.AvailableConfig;
import com.techq.available.quorum.Election;
import com.techq.available.quorum.Notification;
import com.techq.available.quorum.Notification.mType;

public class FollowerHandler3 {
	private static final Logger LOG = LoggerFactory.getLogger(FollowerHandler3.class);
	volatile boolean confirmed = false;
	int tick = 0;
	Election election;
	long leaderId = -1;
	ConfirmWorker worker;
	boolean hasStartWork = false;
	AtomicLong expectClock = new AtomicLong(0);
	
	public FollowerHandler3(Election election, long leaderId) {
		super();
		this.election = election;
		this.leaderId = leaderId;
		worker = new ConfirmWorker(election);
	}
	
	public boolean followLeader(Notification n) throws InterruptedException {
		n.setLogicalClock(expectClock.addAndGet(1));
		if (!hasStartWork) {
			hasStartWork = true;
			worker.start();
		}
		sendAgree(n);
		confirmed = false;
		tick = 0;
		worker.startPolling();
		while(!confirmed && tick <= AvailableConfig.syncTick) {
			TimeUnit.MILLISECONDS.sleep(AvailableConfig.tickTime);
			tick++;
		}
		worker.finishPolling();
		if (tick <= AvailableConfig.syncTick && confirmed == true) {
			return true;
		}
		LOG.warn("can't receive any confirms!!, restart looking leader!!!");
		return false;
	}

	private void sendAgree(Notification n) {
		election.offerACK(n);
	}
	
	
	class ConfirmWorker extends Thread {
		final Election election;
		volatile boolean finishPoll = false;
		volatile boolean isRunning = true;
		CyclicBarrier barrier = new CyclicBarrier(2);
		
		ConfirmWorker(Election election) {
			this.election = election;
		}
		
		public void run() {
			while(isRunning) {
				waiting();
				try {
					finishPoll = false;
					while(!finishPoll) { 
						Notification n = election.pollConfirm(AvailableConfig.pollTimeout, AvailableConfig.pollTimeUnit);
						if (n != null && n.getFrom() == leaderId && n.getType() == Notification.mType.CONFIRM
						 && n.getLogicalClock() == expectClock.get())
						{
							LOG.info("Got leader confirms");
							finishPoll = true;
							confirmed = true;
							break;
						} else {
							/** if the ticket is not current, i will push back such ticket*/
							election.pushback(n);
						}
					}
					LOG.info("finish polling!");
				} catch (InterruptedException e) {
					LOG.error("interrupt when poll confirms from leader, error=", e);
				}
				
			}
		}
		
		private void waiting() {
			try {
				barrier.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void finishWork() {
			this.isRunning = false;
			finishPolling();
		}
		
		public void finishPolling() {
			if (this.finishPoll) {
				return;
			}
			this.finishPoll = true;
			this.interrupt();
		}
		
		
		public void startPolling() {
			LOG.info("call start polling...");
			//call again
			waiting();
		}

	}

}
