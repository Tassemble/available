package com.forest.ape.mq.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

public class SimpleSendWorker extends SendWorkerMQ {

	
	final String TASK_QUEUE_NAME = "task_queue";
	
	 
	
	@Override
	protected SendWorkerMQ createPublisher() throws IOException {
		
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		connection = factory.newConnection();
		channel = connection.createChannel();

		//set it durable so that it can't be lost when mq restart it
		boolean durable = true;
		channel.queueDeclare(TASK_QUEUE_NAME, durable, false, false, null);
		return this;
	}



	@Override
	public void publish(ByteBuffer buf) throws IOException {
		channel.basicPublish("", TASK_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN,
				buf.array());
	}
	
	
	
	@Override
	public void close() throws IOException {
		channel.close();
		connection.close();
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}	
	

}
