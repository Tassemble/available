package com.forest.ape.mq.impl;

import java.util.concurrent.TimeUnit;

import com.forest.ape.mq.CallableHandler;

public class DefaultSentHandler implements CallableHandler.AsynSentHandler{

	volatile long total = 0;
	
	Statictis t = new Statictis();
	
	@Override
	public void handleAck(boolean isOK) {
		if (!t.isAlive())
			t.start();
		if (isOK) {
			total++;
//			try {
//				TimeUnit.SECONDS.sleep(1);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		else 
			System.err.println("not ok");
	}
	
	
	class Statictis extends Thread {
		StringBuilder sb = new StringBuilder();
		int time = 1;
		long last = 0;
		boolean running = true;
		@Override
		public void run() {
			while(running) {
				long cur = total;
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
