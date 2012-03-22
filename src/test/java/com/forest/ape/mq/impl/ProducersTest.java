package com.forest.ape.mq.impl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import com.forest.ape.mq.CallableHandler;

public class ProducersTest {

	@Ignore
	@Test
	public void test() throws IOException, InterruptedException {
		ProducersGaurd gaurd = new ProducersGaurd();
		CallableHandler.AsynSentHandler handler = new CallableHandler.AsynSentHandler() {
			volatile long total = 0;
			
			Thread t = new Statictis();
			@Override
			public void handleAck(boolean isOK) {
				if (!t.isAlive())
					t.start();
				if (isOK)
					total++;
				else 
					System.err.println("not ok");
			}
			
			
			class Statictis extends Thread {
				StringBuilder sb = new StringBuilder();
				int time = 1;
				long last = 0;
				@Override
				public void run() {
					while(true) {
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
			}
		};
		
		for (int i = 0; i < 3000; i++) {
			boolean ret = gaurd.appendData("/hello/world", new MQPacket("abc".getBytes(), handler));
			if (!ret) {
				System.out.println("retry append");
				gaurd.appendData("/hello/world", new MQPacket("abc".getBytes(), handler));
			}
		}
		
		TimeUnit.SECONDS.sleep(10);
		
		
		for (int i = 0; i < 3000; i++) {
			boolean ret = gaurd.appendData("/hello/world", new MQPacket("abc".getBytes(), handler));
			if (!ret) {
				System.out.println("retry append");
				gaurd.appendData("/hello/world", new MQPacket("abc".getBytes(), handler));
			}
		}
		
		
		gaurd.join();
	}
}
