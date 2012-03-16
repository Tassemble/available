package com.forest.ape.mq.impl;


import java.io.IOException;
import java.nio.ByteBuffer;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;

/**
 * 
 * @author CHQ
 * 2012-2-8
 * 
 * this is only for leader to send packets to rabbitMQ
 */
public abstract class SendWorkerMQ {

	Channel channel;
	Connection connection;
	
	public static SendWorkerMQ createMQWorker() throws Exception {
		try {
			SendWorkerMQ mq = (SendWorkerMQ)Class.forName(SimpleSendWorker.class.getName()).newInstance();
			return mq.createPublisher();
		} catch (Exception e) {
            Exception ie = new Exception("Couldn't instantiate "
                    + SimpleSendWorker.class.getName());
            ie.initCause(e);
            throw ie;
        }
	}
	
	
	protected abstract SendWorkerMQ createPublisher() throws IOException;
	
	public abstract void publish(ByteBuffer buf) throws IOException;
	
	public abstract void close() throws IOException;
	

	private static String getMessage(String[] strings) {
		if (strings.length < 1)
			return "Hello World!";
		return joinStrings(strings, " ");
	}

	private static String joinStrings(String[] strings, String delimiter) {
		int length = strings.length;
		if (length == 0)
			return "";
		StringBuilder words = new StringBuilder(strings[0]);
		for (int i = 1; i < length; i++) {
			words.append(delimiter).append(strings[i]);
		}
		return words.toString();
	}

	// ...
}