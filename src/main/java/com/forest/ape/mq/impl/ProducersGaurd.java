package com.forest.ape.mq.impl;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.*;
import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * 
 * @author CHQ
 * 2012-3-22
 */
public class ProducersGaurd {
	static Logger LOG = LoggerFactory.getLogger(ProducersGaurd.class);
	 ConcurrentHashMap<String, MessageProducer> queues = new ConcurrentHashMap<String, MessageProducer>();
	 /** use for queue name for consumers */
	 HashMap<String, MessageProducer> namingQueues = new HashMap<String, MessageProducer>();
	 AtomicLong totalProducers = new AtomicLong(0);
	 ConnectionFactory connectionFactory;
	 static  String EXCHANGE_NAME = "direct_data";
	 volatile boolean throttleAll = false;
	 ProducerConfig config;
	 String defaultQueueName = "/!/NamingQueue_";
	 
	
	public ProducersGaurd() throws IOException {
		super();
		connectionFactory = new ConnectionFactory();
		config = new ProducerConfig();
		config.loadConfig();
		
		for (String consumerName : config.consumers) {
			consumerName = defaultQueueName + consumerName;
			MessageProducer pro = new MessageProducer(consumerName, config);
			pro.start();
			LOG.info("create default queue:" + consumerName);
			namingQueues.put(consumerName, pro);
		}
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
			
			for (Map.Entry<String, MessageProducer> pro : namingQueues.entrySet()) {
				String queueName = pro.getKey() + path;
				LOG.info("create path(queue) :" + queueName);
				pro.getValue().addPacket(new MQPacket(queueName.getBytes("UTF-8"), null));
			}
			queues.put(path, mp);
			totalProducers.addAndGet(1);
			mp.start();
		}
		return mp.addPacket(packet);
	}
	
	
	
	class MessageProducer extends Thread {
		final Logger LOG = LoggerFactory.getLogger(MessageProducer.class);
		private BlockingQueue<MQPacket> outstandingQueue = new LinkedBlockingDeque<MQPacket>();
		volatile boolean isRunning = true;
		Channel ch;
		Connection conn;
		final String bindName;
		final List<String> queues = new ArrayList<String>();
		int idleLimit = 60; 
		volatile boolean throttle = false;
		
		/** one binding to several queues for one tree node*/
		MessageProducer(String queueName, ProducerConfig config) throws IOException {
			super("queue[" + queueName + "]");
			idleLimit = config.queueIdleLimit;
			bindName = queueName;
			
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
			 *  exchange - the name of the exchange
				type - the exchange type direct topic fanout headers
				durable - true if we are declaring a durable exchange (the exchange will survive a server restart)
			 */
			ch.exchangeDeclare(EXCHANGE_NAME, "direct", true);
			
			
			/**
			 *  queue - the name of the queue
				durable - true if we are declaring a durable queue (the queue will survive a server restart)
				exclusive - true if we are declaring an exclusive queue (restricted to this connection)
				autoDelete - true if we are declaring an autodelete queue (server will delete it when no longer in use)
				arguments - other properties (construction arguments) for the queue
			 */
			if (queueName.startsWith(defaultQueueName)) {
				ch.queueDeclare(queueName, true, false, false, haPolicy);
				ch.queueBind(queueName, EXCHANGE_NAME, bindName);
				queues.add(queueName);
			} else {
				for (String each : namingQueues.keySet()) {
					String rename = each  + queueName;
					ch.queueDeclare(rename, true, false, false, haPolicy);
					
					/**
					 * queue name
					 * exchange name
					 * bind name
					 */
					ch.queueBind(rename, EXCHANGE_NAME, bindName);
					queues.add(rename);
				}
			}
			
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
						LOG.debug("queue " + queues.toString() + " is idle[time="+idle+"]"); 
						if (idle == idleLimit) {
							throttle = true;
							LOG.info("close queue " + queues.toString() + " thread!");
							shutdown();
						}
						continue;
					} else {
						idle = 0;
					}
//					MQPacket p = outstandingQueue.take();
					ch.basicPublish(EXCHANGE_NAME, bindName, config.mp, p.data);
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
			
			queues.remove(bindName);
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
		BasicProperties mp;
		int queueIdleLimit = 60;
		boolean persistent = true;
		List<String> consumers = null;
		
		//List<String> consumerIDs = new ArrayList<String>();
		
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
				LOG.info("Queue Idle Limit is " + queueIdleLimit);
				
				
				persistent  = Boolean.valueOf((value = p.get("Persistent")) != null ? value.toString() : null);
				LOG.info("Queue persistent is " + persistent);
				if (persistent == true)
					mp = MessageProperties.PERSISTENT_BASIC;
				else {
					mp = MessageProperties.BASIC;
				}
				
				
				value = (value = p.get("Consumers")) != null ? value.toString() : null;
				if (value != null) {
					consumers = Arrays.asList(value.toString().split(":"));
					for (String string : consumers) {
						LOG.info("consumer id:" + string);
					}
				}
				assert(consumers != null);
			} catch (Throwable e) {
				LOG.error("config parse error!", e);
			}
			
			
		}
	}
}
