package com.forest.ape.mq.impl;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.*;
import com.rabbitmq.client.*;

/**
 * 
 * @author CHQ
 * 2012-3-22
 */
public class ProducersGaurd {
	static Logger LOG = LoggerFactory.getLogger(ProducersGaurd.class);
	 ConcurrentHashMap<String, MessageProducer> queues = new ConcurrentHashMap<String, MessageProducer>();
	 AtomicLong totalProducers = new AtomicLong(0);
	 ConnectionFactory connectionFactory;
	 static  String EXCHANGE_NAME = "direct_data";
	 volatile boolean throttleAll = false;
	 ProducerConfig config;
	
	
	
	public ProducersGaurd() {
		super();
		connectionFactory = new ConnectionFactory();
		config = new ProducerConfig();
		config.loadConfig();
	}


	public void clearAll() throws IOException {
		throttleAll = true;
		for (MessageProducer entry : queues.values()) {
			entry.shutdown();
		}
		queues.clear();
		totalProducers.set(0);
	}
	
	
	public boolean appendData(String path, MQPacket packet) throws IOException {
		//if append is closed, so can't do nothing
		if (throttleAll) {
			LOG.info("append is closed!");
			return false;
		}
		
		if (path == null || packet == null)
		{
			LOG.warn("path can't be null or packet can't be null");
			return false;
		}
		
		MessageProducer mp = queues.get(path);
		if (mp == null) {
			mp = new MessageProducer(path, config);
			totalProducers.addAndGet(1);
			queues.put(path, mp);
			mp.start();
		}
		return mp.addPacket(packet);
	}
	
	
	
	class MessageProducer extends Thread {
		final Logger LOG = LoggerFactory.getLogger(MessageProducer.class);
		private BlockingQueue<MQPacket> outstandingQueue = new ArrayBlockingQueue<MQPacket>(100000);
		volatile boolean isRunning = true;
		Channel ch;
		Connection conn;
		final String bindName;
		final String queue;
		int idleLimit = 60; 
		volatile boolean throttle = false;
		
		MessageProducer(String queueName, ProducerConfig config) throws IOException {
			super("queue[" + queueName + "]");
			idleLimit = config.queueIdleLimit;
			bindName = queueName;
			queue= queueName;
			LOG.info("start MessageProducer...");
			conn = connectionFactory.newConnection(config.addrArr);
			ch = conn.createChannel();
			// x-ha-policy

			Map<String, Object> haPolicy = new HashMap<String, Object>();
			haPolicy.put("x-ha-policy", "nodes");
			haPolicy.put("x-ha-policy-params", config.asList);

			// first durable
			if (!config.enableHA)
				haPolicy = null;
			
			/**
			 *  queue - the name of the queue
				durable - true if we are declaring a durable queue (the queue will survive a server restart)
				exclusive - true if we are declaring an exclusive queue (restricted to this connection)
				autoDelete - true if we are declaring an autodelete queue (server will delete it when no longer in use)
				arguments - other properties (construction arguments) for the queue
			 */
			ch.queueDeclare(queueName, true, false, false, haPolicy);
			/**
			 *  exchange - the name of the exchange
				type - the exchange type direct topic fanout headers
				durable - true if we are declaring a durable exchange (the exchange will survive a server restart)
			 */
			ch.exchangeDeclare(EXCHANGE_NAME, "direct", true);
			
			/**
			 * queue name
			 * exchange name
			 * bind name
			 */
			ch.queueBind(queueName, EXCHANGE_NAME, queueName);
			
			/**
			 * 
			
			exchange - the exchange to publish the message to
			routingKey - the routing key
			props - other properties for the message - routing headers etc
			body - the message body
			
			 */
//			channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes());
		}
		
		@Override
		public void run() {
			
			int idle = 0;
			while(isRunning && !throttle) {
				try {
					MQPacket p = outstandingQueue.poll(1, TimeUnit.SECONDS);
					if (p == null) {
						idle++;
						LOG.info("queue[" + queue + "] is idle[time="+idle+"]"); 
						if (idle == idleLimit) {
							throttle = true;
							LOG.info("close queue[" + queue + "] thread!");
							shutdown();
						}
						continue;
					} else {
						idle = 0;
					}
//					MQPacket p = outstandingQueue.take();
					ch.basicPublish(EXCHANGE_NAME, bindName, MessageProperties.PERSISTENT_BASIC, p.data);
					if (p.handler != null) {
						p.handler.handleAck(ch.waitForConfirms());
					}
				} catch (InterruptedException e) {
					LOG.warn("", e);
				} catch (IOException e) {
					LOG.warn("", e);
				}
			}
		}
		
		
		public void shutdown() throws IOException {
			LOG.info("#shutdown");
			/** refuse accepting any packet */
			throttle = true;
			
			queues.remove(queue);
			if (isRunning == false)
				return;
			isRunning = false;
			this.interrupt();
			ch.close();
			conn.close();
			
		}
		
		public boolean addPacket(MQPacket p) {
			if (!throttle)
				return outstandingQueue.add(p);
			LOG.warn("throttle is closed!!! packet is not adding, so you should retry it!");
			return false;
		}
		
		
	}
	
	
	
	public void join() throws InterruptedException {
		for (MessageProducer entry : queues.values()) {
			entry.join();
		}
	}
	
	
	class ProducerConfig {
		Logger LOG = LoggerFactory.getLogger(ProducerConfig.class);
		Address[] addrArr;
		boolean enableHA = false;
		List<String> asList = null;
		MessageProperties mp;
		int queueIdleLimit = 60;
		
		public void loadConfig() {
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
					asList= Arrays.asList(haPolicy.split(";")[1].split(","));
					for (String string : asList) {
						LOG.info("MQ ha node:" + string);
					}
				} else {
					enableHA = false;
					LOG.info("disable HA");
				}
				queueIdleLimit  = Integer.valueOf((value = p.get("QueueIdle")) != null ? value.toString() : null);
			} catch (Throwable e) {
				LOG.error("config parse error!", e);
			}
			
			
		}
	}
}
