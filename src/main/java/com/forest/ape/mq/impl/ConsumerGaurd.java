package com.forest.ape.mq.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.mq.CallableHandler;
import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * 
 * @author CHQ
 * 2012-3-23
 */
public class ConsumerGaurd {

	String safeClosing = "safe closing";
	
	CallableHandler.RecvHandler handle;
	ConcurrentHashMap<String, MessageConsumer> queues = new ConcurrentHashMap<String, MessageConsumer>();
	AtomicLong totalConsumers = new AtomicLong(0);
	ConnectionFactory connectionFactory;
	ConsumerConfig config;
	QueueNameConsumer namingConsumer;
	
	/** queue name = defaultQueueName + sid */
	String defaultQueueName = "/!/NamingQueue_";
	
	public ConsumerGaurd(CallableHandler.RecvHandler handle) throws IOException {
		super();
		this.handle = handle;
		connectionFactory = new ConnectionFactory();
		config = new ConsumerConfig();
		config.loadConfig();
		namingConsumer = new QueueNameConsumer(config);
	}

	
	/**
	 * each consumer is matching each Treenode if consumer closed it won't receive any msg for such tree node
	 * two condition will lead to this closing:
	 * 	close the cluster
	 */
	class MessageConsumer extends Thread {
		/** queue name is equal with path*/
		final String path;
		volatile boolean isRunning = true;
		Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);
		Connection conn;
		Channel ch;
		QueueingConsumer qc;
		
		
		
		public MessageConsumer(String queueName, ConsumerConfig config) throws IOException {
			super("MessageConsumer");
			this.path = queueName;
			conn = connectionFactory.newConnection(config.addrArr);
			ch = conn.createChannel();
			Map<String, Object> haPolicy = new HashMap<String, Object>();
			haPolicy.put("x-ha-policy", "nodes");
			haPolicy.put("x-ha-policy-params", config.asList);
			
			if (!config.enableHA) {
				haPolicy = null;
			}
			
			ch.queueDeclare(queueName, true, false, false, haPolicy);
			// Consume
			qc = new QueueingConsumer(ch);
			// do not apply auto ack true
			ch.basicConsume(queueName, false, qc);
		}
		
		

		@Override
		public void run() {
			while(isRunning) {
				try {
					QueueingConsumer.Delivery delivery = qc.nextDelivery();
					// append to
					if (LOG.isDebugEnabled()) {
						LOG.debug("processing msg...:"
								+ delivery.getEnvelope().getDeliveryTag());
					}
					
					boolean result = handle.handleRecv(delivery.getBody());
					
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
				} catch (ShutdownSignalException e) {
					LOG.error("", e);
				} catch (ConsumerCancelledException e) {
					LOG.error("", e);
				} catch (InterruptedException e) {
					LOG.error("", e);
				} catch (IOException e) {
					LOG.error("", e);
				}
			}
			
		}
		
		public void shutdown(String cmd) throws IOException {
			if (cmd.equals("safe closing")) {
				if (isRunning == false)
					return;
				isRunning = false;
				this.interrupt();
				ch.close();
				conn.close();
			} else {
				LOG.warn("can't close queue:[" + path + "] for not safe closing");
			}
		}
	}
	
	public void start() throws IOException {
		namingConsumer.start();
	}
	
	
	public void join() throws InterruptedException {
		namingConsumer.join();
		for (MessageConsumer entry : queues.values()) {
			entry.join();
		}
	}
	
	public void close() {
		
	}
	
	/**
	 * this class is using for achieving queue name create by leader
	 */
	class QueueNameConsumer extends Thread {
		Logger LOG = LoggerFactory.getLogger(QueueNameConsumer.class);
		boolean isRunning = true;
		/** this queue is default for achieving the queues create by leader*/
		
		Connection conn;
		Channel ch;
		QueueingConsumer qc;
		ConsumerConfig config;
		
		public QueueNameConsumer(ConsumerConfig config) throws IOException {
			super("QueueNameConsumer");
			this.config = config;
			defaultQueueName += config.serverID;
			conn = connectionFactory.newConnection(config.addrArr);
			ch = conn.createChannel();
			Map<String, Object> haPolicy = new HashMap<String, Object>();
			haPolicy.put("x-ha-policy", "nodes");
			haPolicy.put("x-ha-policy-params", config.asList);
			
			if (!config.enableHA) {
				haPolicy = null;
			}
			
			ch.queueDeclare(defaultQueueName, true, false, false, haPolicy);
			// Consume
			qc = new QueueingConsumer(ch);
			// do not apply auto ack true
			ch.basicConsume(defaultQueueName, false, qc);
		}
		
		
		@Override
		public void run(){
			LOG.info("start QueueNameConsumer thread");
			while(isRunning) {
				try {
					QueueingConsumer.Delivery delivery = qc.nextDelivery();
					// append to
					if (LOG.isDebugEnabled()) {
						LOG.debug("processing msg...:"
								+ delivery.getEnvelope().getDeliveryTag());
					}
					
					boolean result = handle.handleRecv(delivery.getBody());
					MessageConsumer mc = createMessageConsumer(delivery.getBody(), config);
					mc.start();
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("put node to map:" + mc.path.substring(defaultQueueName.length()));
					}
					queues.put(mc.path.substring(defaultQueueName.length()), mc);
					
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
				} catch (ShutdownSignalException e) {
					LOG.error("", e);
				} catch (ConsumerCancelledException e) {
					LOG.error("", e);
				} catch (InterruptedException e) {
					LOG.error("", e);
				} catch (IOException e) {
					LOG.error("", e);
				}
			}
		}
		
		public void shutdown() throws IOException {
			LOG.info("close QueueNameConsumer thread!");
			if (isRunning == false)
				return;
			isRunning = false;
			this.interrupt();
			ch.close();
			conn.close();
		}
		
		
		public MessageConsumer createMessageConsumer(byte[] data, ConsumerConfig config) throws IOException {
			String path = new String(data);
			LOG.info("create MessageConsumer, node path:" + path);
			return new MessageConsumer(path, config);
		}
	}
	
	
	class ConsumerConfig {
		Logger LOG = LoggerFactory.getLogger(ConsumerConfig.class);
		Address[] addrArr;
		boolean enableHA = false;
		List<String> asList = null;
		BasicProperties mp;
		int queueIdleLimit = 60;
		boolean persistent = true;
		String serverID = "-1";
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
					LOG.info("MQ node addr:" + addr);
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
				
				serverID  = (value = p.get("ServerID")) != null ? value.toString() : null;
				LOG.info("serverID:"+ serverID);
			} catch (Throwable e) {
				LOG.error("config parse error!", e);
			}
			
			
		}
	}
	
}
