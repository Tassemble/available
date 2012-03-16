package com.techq.available.quorum.together;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
	private static final Logger LOG = LoggerFactory.getLogger(App.class);
	
	public static void main(String[] args) throws InterruptedException {
		LOG.info("start---");
		Runnable leader = new Runnable() {
			@Override
			public void run() {
				try {
					new LeaderTest().test1();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
		};
		Runnable follow1 = new Runnable() {
			public void run(){
				try {
					new FollowerTest().test1();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
		
		Runnable follow2 = new Runnable() {
			public void run(){
				try {
					new FollowerTest().test2();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		Thread leaderThread = new Thread(leader, "Leader[myid=2]");
		Thread followThread1 = new Thread(follow1, "Follow1[myid=1]");
		Thread followThread2 = new Thread(follow2, "Follow2[myid=3]");
		leaderThread.start();
		followThread1.start();
		followThread2.start();
		
		try {
			leaderThread.join();
			followThread1.join();
			followThread2.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
