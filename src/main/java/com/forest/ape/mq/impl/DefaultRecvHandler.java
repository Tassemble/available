package com.forest.ape.mq.impl;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.forest.ape.mq.CallableHandler.RecvHandler;

public class DefaultRecvHandler implements RecvHandler {
	Logger LOG = LoggerFactory.getLogger(DefaultRecvHandler.class);
	
	
	@Override
	public boolean handleRecv(byte[] data) {
		
		if (!t.isAlive())
			t.start();
		total.addAndGet(1);
//			try {
//				TimeUnit.SECONDS.sleep(1);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		return true;
	}
	

	AtomicLong total = new AtomicLong(0);
	
	Statictis t = new Statictis();

	
	
	class Statictis extends Thread {
		StringBuilder sb = new StringBuilder();
		int time = 1;
		long last = 0;
		boolean running = true;
		@Override
		public void run() {
			while(running) {
				long cur = total.get();
				sb.append("time:" + time + "s " + (cur - last));
				System.out.println("time:" + time + "s " + (cur - last));
				last = cur;
				try {
					TimeUnit.SECONDS.sleep(1);
					time++;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		public void shutdown() {
			running = false;
			this.interrupt();
		}
	}
	
	public void shutdown() {
		t.shutdown();
	}

}
