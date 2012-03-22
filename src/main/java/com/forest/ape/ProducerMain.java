package com.forest.ape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.forest.ape.mq.impl.DefaultSentHandler;
import com.forest.ape.mq.impl.MQPacket;
import com.forest.ape.mq.impl.ProducersGaurd;

/**
 * 
 * @author chq
 */
public class ProducerMain {

	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		new ProducerMain().test(Long.valueOf(1000000));
	}
	
	
	public void test(long size) throws IOException, InterruptedException {
		ProducersGaurd gaurd = new ProducersGaurd();
		List<String> queueList = new ArrayList<String>();
 		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < 1000; i++) {
			buf.append('a');
		}
		byte[] data = buf.toString().getBytes("utf-8");
		
		
		
		DefaultSentHandler handler = new DefaultSentHandler();
		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < size; i++) {
			String ranQueue;
			if (queueList.size() < 100)
			{	ranQueue= String.valueOf(System.currentTimeMillis());
				queueList.add(ranQueue);
			} else {
				ranQueue = queueList.get(r.nextInt(queueList.size()));
			}
			boolean ret = gaurd.appendData(ranQueue, new MQPacket(data, handler));
			if (!ret) {
				System.out.println("retry append");
				gaurd.appendData(ranQueue, new MQPacket(data, handler));
			}
		}
		
//		TimeUnit.SECONDS.sleep(8);
//		
//		
//		for (int i = 0; i < 1000000; i++) {
//			boolean ret = gaurd.appendData("/hello/world", new MQPacket("abc".getBytes(), handler));
//			if (!ret) {
//				System.out.println("retry append");
//				gaurd.appendData("/hello/world", new MQPacket(data, handler));
//			}
//		}
		gaurd.join();
		System.out.println("end");
		handler.shutdown();
	}

}
