package com.forest.ape.mq.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.mq.CallableHandler;
import com.forest.ape.mq.CallableHandler.AsynSentHandler;
import com.forest.ape.mq.OutBridge;
import com.forest.ape.server.persistence.Request;
import com.rabbitmq.client.*;


/**
 * 
 * @author CHQ
 * 2012-3-16
 */
public class SendWorker extends Thread {
	static Logger LOG = LoggerFactory.getLogger(SendWorker.class);

	String EXCHANGE = "EXCHANGE";
	static List<String> asList;/*
						 * = Arrays.asList("hare@app-20", "rabbit@app-20"/* ,
						 * "GreyBit@app-20" );
						 */
	boolean throttle = false;

	List<String> learners;
	boolean isRunning = true;
	static ConnectionFactory connectionFactory;
	static Address[] addrArr;
	int retry = 0;
	static boolean enableHA = false;
	String topic = "#";
//	private volatile SortedSet<Long> unconfirmedSet = Collections
//			.synchronizedSortedSet(new TreeSet());
	private BlockingQueue<MQPacket> outstandingQueue = new LinkedBlockingDeque<MQPacket>();
	OutBridge bridge;
	String epoch;
	Connection conn;
	Channel ch;
	static {
		SendWorker.loadConfig();
	}
	
	
	/**
	 *  each time create a new exchange and queue for new leader and new followers
	 * @param leaderId
	 * @param followers
	 * @param epoch
	 */
	public SendWorker(String leaderId, List<String> followers, String epoch) {
		super("SendWorker");
		this.EXCHANGE = this.EXCHANGE + "_" +  epoch;
		this.epoch = epoch;
		this.learners = followers;
		this.bridge = new DefaultOutBridge();
		connectionFactory = new ConnectionFactory();
	}
	
	

	@Override
	public void run() {
		try {
			// Setup
			LOG.info("start sendworker...");
			conn = connectionFactory.newConnection(addrArr);
			ch = conn.createChannel();
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

//			ch.addConfirmListener(new ConfirmListenerHandler());

			ch.confirmSelect();

			queueBinds(ch, learners, EXCHANGE, topic);

			// third durable
			// ch.basicPublish(EXCHANGE, "anomoy",
			// MessageProperties.PERSISTENT_BASIC, message.getBytes());
			while(isRunning) {
				MQPacket packet = outstandingQueue.take();
				bridge.enmq(EXCHANGE, ch, packet);
				if (packet.handler != null) {
					packet.handler.handleAck(ch.waitForConfirms());
				}
			}
//			ch.waitForConfirmsOrDie();

		} catch (InterruptedException e) {
			LOG.warn("", e);
		} catch (Throwable e) {
			LOG.info("foobar", e);
		}
	}

	
	public boolean enQueue(MQPacket packet) {
		if (!throttle) {
			return outstandingQueue.add(packet);
		}
		return false;
	}
	
	
	private void queueBinds(Channel ch, List<String> learners2,
			String exchange2, String topic2) throws IOException {
		for (String learner : learners2) {
			ch.queueBind(learner + "_" + epoch, exchange2, topic2);
		}

	}

	private void declareQueues(Channel ch, List<String> learners,
			Map<String, Object> haPolicy) throws IOException {
		for (String string : learners) {
			ch.queueDeclare(string + "_" + epoch, true, false, false, haPolicy);
		}

	}

//	class ConfirmListenerHandler implements ConfirmListener {
//
//
//		public void handleAck(long seqNo, boolean multiple) {
//			if (multiple) {
//				unconfirmedSet.headSet(seqNo + 1).clear();
//			} else {
//				unconfirmedSet.remove(seqNo);
//			}
//		}
//
//		public void handleNack(long seqNo, boolean multiple) {
//			// handle the lost messages somehow
//		}
//	}
	
	
	public void shutdown() throws InterruptedException, IOException {
		LOG.warn("#shutdown#:mq sender");
		throttle = true;
		
		while(outstandingQueue.size() > 0) {
			TimeUnit.SECONDS.sleep(1);
		}
		isRunning = false;
		this.interrupt();
		
		ch.close();
		conn.close();
	}
	
	public static void loadConfig() {
		try {
			String config = "conf/config";
			LOG.info("load Sender config: " + config);
			Properties p = new Properties();
			p.load(new InputStreamReader(new FileInputStream(
					config), "UTF-8"));
			Object value;
			String addrs = (value = p.get("MQClusterAddrs")) != null ? value.toString() : null;
			String[] manyAddrs = addrs.split(";");
			addrArr = new Address[manyAddrs.length];
			int cnt = 0;
			for (String addr : manyAddrs) {
				addrArr[cnt++] = new Address(addr.split(":")[0], Integer.valueOf(addr.split(":")[1]));
				LOG.info("MQ node:" + addr);
			}
			String haPolicy = (value = p.get("MQCluster-ha-policy")) != null ? value.toString() : null;
			String haPolicyDecision = haPolicy.split(";")[0];
			if (haPolicyDecision.equalsIgnoreCase("true")) {
				enableHA = true;
				LOG.info("enable HA");
				asList = Arrays.asList(haPolicy.split(";")[1].split(","));
				for (String string : asList) {
					LOG.info("MQ ha node:" + string);
				}
			} else {
				enableHA = false;
				LOG.info("disable HA");
			}
		} catch (Throwable e) {
			LOG.error("config parse error!", e);
		}
		
		
	}

}
