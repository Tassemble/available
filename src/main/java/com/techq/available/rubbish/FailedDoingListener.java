package com.techq.available.rubbish;

import java.util.concurrent.CountDownLatch;


public class FailedDoingListener implements CallableListener {

	CountDownLatch latch;
	public FailedDoingListener(CountDownLatch latch) {
		this.latch = latch;
	}
	
	
	
	@Override
	public void doit(Object o) {
		
	}

	@Override
	public void failedDo(Object o) {
		this.latch.countDown();
	}

	@Override
	public void succeededDo(Object o) {
		// TODO Auto-generated method stub
		
	}

	


}
