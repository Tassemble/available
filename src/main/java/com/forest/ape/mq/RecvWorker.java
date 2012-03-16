package com.forest.ape.mq;

import java.util.*;

import com.rabbitmq.client.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author CHQ 2012-3-14
 * 
 *         always extract changable things
 */

public class RecvWorker extends Thread {
	Logger LOG = LoggerFactory.getLogger(RecvWorker.class);

	final static String Follower2 = "Follower2Queue";
	final static String Follower1 = "Follower1Queue";
	final static String QUEUE_NAME = Follower1;
	final static String EXCHANGE = "EXCHANGE";
	List<String> asList;/* = Arrays.asList("hare@app-20", "rabbit@app-20"/*
																	 * ,
																	 * "GreyBit@app-20"
																	 );*/
	boolean isRunning = true;
	static ConnectionFactory connectionFactory;
	Address[] addrArr;
	DataBridge dataHelper;
	int retry = 0;
	
	public RecvWorker(List<String> asList, Address[] addrArr,
			DataBridge dataHelper) {
		super();
		this.asList = asList;
		this.addrArr = addrArr;
		this.dataHelper = dataHelper;
		connectionFactory = new ConnectionFactory();
	}

	public void run() {
		try {
			Connection conn = connectionFactory.newConnection(addrArr);
			Channel ch = conn.createChannel();
			Map<String, Object> haPolicy = new HashMap<String, Object>();
			haPolicy.put("x-ha-policy", "nodes");
			haPolicy.put("x-ha-policy-params", asList);
			ch.queueDeclare(QUEUE_NAME, true, false, false, haPolicy);
			ch.exchangeDeclare(EXCHANGE, "topic", true);
			// Consume
			QueueingConsumer qc = new QueueingConsumer(ch);
			//do not apply auto ack true
			ch.basicConsume(QUEUE_NAME, false, qc);
			boolean result = false;
			while (isRunning) {
				 
				//after we write log to disk
				 QueueingConsumer.Delivery delivery = qc.nextDelivery();
				 //append to 
				 if (LOG.isDebugEnabled()) {
					 LOG.debug("processing msg...:" + delivery.getEnvelope().getDeliveryTag());
				 }
				 result = dataHelper.append(delivery);
				 result = dataHelper.commit() && result;
				//doing some thing about delivery
				 if (result) {
					 if (LOG.isDebugEnabled()) {
						 LOG.debug("process msg ok:" + delivery.getEnvelope().getDeliveryTag());
					 }
					 ch.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
				 } else {
					 LOG.warn("recv msg failed:" + delivery.getEnvelope().getDeliveryTag());
				 }
			}
			ch.close();
			conn.close();
		} catch (Throwable e) {
			LOG.error("Whoosh!", e);
		}
	}

	public void shutdown() {
		isRunning = false;
		this.interrupt();
	}

}
