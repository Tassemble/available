package com.techq.available.quorum.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.mq.impl.SendWorkerMQ;
import com.techq.available.AvailableConfig;
import com.techq.available.data.BasicPacket;

public class DataHandler extends Thread {
	private static final Logger LOG = LoggerFactory
			.getLogger(DataHandler.class);
	LinkedBlockingQueue<BasicPacket> mqQueue = new LinkedBlockingQueue<BasicPacket>();
	boolean isRunning = true;
	SendWorkerMQ mq;
	Leader leader;
	
	public DataHandler(Leader leader) throws Exception {
		super("DataHandler");
		this.leader = leader;
		mq = SendWorkerMQ.createMQWorker();
	}

	public void run() {

		while (isRunning) {
			BasicPacket n = null;
			try {
				n = mqQueue.poll(AvailableConfig.pollTimeout,
						AvailableConfig.pollTimeUnit);

				if (n == null) {
					n = mqQueue.take();
				}
				mq.publish(ByteBuffer.wrap(n.getData()));

			} catch (InterruptedException e) {
				LOG.error(
						"sending data to mq, ",
						e);
			} catch (IOException e) {
				LOG.error(
						"sending data to mq, ",
						e);
			}
			
		}

	}

	public void halt() {
		if (!isRunning)
			return;
		isRunning = false;
		this.interrupt();
	}
}
