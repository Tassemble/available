package com.forest.ape.mq.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import com.forest.ape.mq.CallableHandler;
import com.forest.ape.mq.InBridge;
import com.forest.ape.server.ZooDefs.OpCode;
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
	static Logger LOG = LoggerFactory.getLogger(RecvWorker.class);

	final static String Follower2 = "Follower2Queue";
	final static String Follower1 = "Follower1Queue";
	static String QUEUE_NAME = Follower1;
	static String EXCHANGE = "EXCHANGE";
	static List<String> asList;/*
								 * = Arrays.asList("hare@app-20",
								 * "rabbit@app-20"/* , "GreyBit@app-20" );
								 */
	static boolean enableHA = false;
	CallableHandler handler;
	boolean isRunning = true;
	static ConnectionFactory connectionFactory;
	static Address[] addrArr;
	InBridge dataHelper;
	int retry = 0;
	
	static {
		RecvWorker.loadConfig();
	}

	public RecvWorker(List<String> asList, Address[] addrArr,
			InBridge dataHelper) {
		super();
		this.dataHelper = dataHelper;
		connectionFactory = new ConnectionFactory();
	}

	public RecvWorker(String Id, String epoch) {
		super("RecvWorker");
		connectionFactory = new ConnectionFactory();
		EXCHANGE = EXCHANGE + "_" +  epoch;
		QUEUE_NAME = Id + "_" + epoch;
	}

	public void run() {
		try {
			Connection conn = connectionFactory.newConnection(addrArr);
			Channel ch = conn.createChannel();
			Map<String, Object> haPolicy = new HashMap<String, Object>();
			haPolicy.put("x-ha-policy", "nodes");
			haPolicy.put("x-ha-policy-params", asList);
			
			if (!enableHA) {
				haPolicy = null;
			}
			
			ch.queueDeclare(QUEUE_NAME, true, false, false, haPolicy);
			ch.exchangeDeclare(EXCHANGE, "topic", true);
			// Consume
			QueueingConsumer qc = new QueueingConsumer(ch);
			// do not apply auto ack true
			ch.basicConsume(QUEUE_NAME, false, qc);
			boolean result = false;
			while (isRunning) {
				// after we write log to disk
				QueueingConsumer.Delivery delivery = qc.nextDelivery();
				// append to
				if (LOG.isDebugEnabled()) {
					LOG.debug("processing msg...:"
							+ delivery.getEnvelope().getDeliveryTag());
				}
				
				result = this.handler.handleRecv(delivery.getBody(), delivery);
				// result = dataHelper.append(delivery);
				// result = dataHelper.commit() && result;
				// doing some thing about delivery
				if (result) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("process msg ok:"
								+ delivery.getEnvelope().getDeliveryTag());
					}
					ch.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
				} else {
					LOG.warn("recv msg failed:"
							+ delivery.getEnvelope().getDeliveryTag());
				}
			}
			ch.close();
			conn.close();
		} catch (Throwable e) {
			LOG.error("Whoosh!", e);
		}
	}
	
	
	public void deleteQueue(Channel ch, String queueName) throws IOException {
		ch.queueDelete(queueName);
	}

	public void shutdown() {
		isRunning = false;
		this.interrupt();
	}

	public static void loadConfig() {
		try {
			String config = "conf/config";
			LOG.info("load Receiver config: " + config);
			Properties p = new Properties();
			p.load(new InputStreamReader(new FileInputStream(config), "UTF-8"));
			Object value;
			String addrs = (value = p.get("MQClusterAddrs")) != null ? value
					.toString() : null;
			String[] manyAddrs = addrs.split(";");
			addrArr = new Address[manyAddrs.length];
			int cnt = 0;
			for (String addr : manyAddrs) {
				addrArr[cnt++] = new Address(addr.split(":")[0],
						Integer.valueOf(addr.split(":")[1]));
				LOG.info("MQ node:" + addr);
			}
			String haPolicy = (value = p.get("MQCluster-ha-policy")) != null ? value
					.toString() : null;
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

	public void setCallHandler(CallableHandler handler) {
		this.handler = handler;
	}
}
