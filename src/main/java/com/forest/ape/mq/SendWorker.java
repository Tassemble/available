package com.forest.ape.mq;

import java.io.IOException;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.server.persistence.Request;
import com.rabbitmq.client.*;

/**
 * 
 * @author chq
 * 
 */

public class SendWorker extends Thread {
	Logger LOG = LoggerFactory.getLogger(RecvWorker.class);

	String EXCHANGE = "EXCHANGE";
	List<String> asList;/*
						 * = Arrays.asList("hare@app-20", "rabbit@app-20"/* ,
						 * "GreyBit@app-20" );
						 */

	List<String> learners;
	boolean isRunning = true;
	static ConnectionFactory connectionFactory;
	Address[] addrArr;
	int retry = 0;
	boolean enableHA = false;
	String topic = "#";
	private volatile SortedSet<Long> unconfirmedSet = Collections
			.synchronizedSortedSet(new TreeSet());
	private BlockingQueue<Request> localQueue = new LinkedBlockingDeque<Request>();
	OutBridge bridge;
	String epoch;
	
	
	
	/**
	 * each time create a new exchange and queue for new leader and new followers
	 * @param asList
	 * @param learners
	 * @param addrArr
	 * @param enableHA
	 * @param topic
	 * @param bridge
	 * @param epoch
	 */
	public SendWorker(List<String> asList, List<String> learners,
			Address[] addrArr, boolean enableHA, String topic, OutBridge bridge, String epoch) {
		super("SendWorker");
		this.asList = asList;
		this.learners = learners;
		this.addrArr = addrArr;
		this.enableHA = enableHA;
		this.topic = topic;
		this.bridge = bridge;
		this.epoch = epoch;
		this.EXCHANGE = this.EXCHANGE + epoch;
	}

	public void run() {
		try {
			// Setup
			Connection conn = connectionFactory.newConnection(addrArr);
			Channel ch = conn.createChannel();
			// x-ha-policy

			Map<String, Object> haPolicy = new HashMap<String, Object>();
			haPolicy.put("x-ha-policy", "nodes");
			haPolicy.put("x-ha-policy-params", asList);

			// first durable
			if (!enableHA)
				haPolicy = null;

			declareQueues(ch, learners, haPolicy);

			// sec durable
			ch.exchangeDeclare(EXCHANGE, "topic", true);

			ch.addConfirmListener(new ConfirmListenerHandler());

			ch.confirmSelect();

			queueBinds(ch, learners, EXCHANGE, topic);

			// third durable
			// ch.basicPublish(EXCHANGE, "anomoy",
			// MessageProperties.PERSISTENT_BASIC, message.getBytes());
			while(isRunning) {
				bridge.enmq(ch, localQueue.take());
			}
			
			ch.waitForConfirmsOrDie();

			ch.close();
			conn.close();

		} catch (Throwable e) {
			System.out.println("foobar");
			System.out.print(e);
		}
	}

	private void queueBinds(Channel ch, List<String> learners2,
			String exchange2, String topic2) throws IOException {
		for (String learner : learners2) {
			ch.queueBind(learner + epoch, exchange2, topic2);
		}

	}

	private void declareQueues(Channel ch, List<String> learners,
			Map<String, Object> haPolicy) throws IOException {
		for (String string : learners) {
			ch.queueDeclare(string + epoch, true, false, false, haPolicy);
		}

	}

	class ConfirmListenerHandler implements ConfirmListener {
		public void handleAck(long seqNo, boolean multiple) {
			if (multiple) {
				unconfirmedSet.headSet(seqNo + 1).clear();
			} else {
				unconfirmedSet.remove(seqNo);
			}
		}

		public void handleNack(long seqNo, boolean multiple) {
			// handle the lost messages somehow
		}
	}

}
