package com.forest.ape.mq.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.mq.OutBridge;
import com.forest.ape.server.persistence.Request;
import com.rabbitmq.client.*;

public class DefaultOutBridge implements OutBridge {
	static Logger LOG = LoggerFactory.getLogger(SendWorker.class);
	
	@Override
	@Deprecated
	public void enmq(Channel ch, MQPacket re) {
		new UnsupportedOperationException();
	}

	@Override
	public void enmq(String exchange, Channel ch, MQPacket re) throws IOException {
		byte[] data = re.data;
		if (LOG.isDebugEnabled()) {
			LOG.debug("send data to mq, length:" + data.length);
		}
		ch.basicPublish(exchange, "anomoy", MessageProperties.PERSISTENT_BASIC, data);
	}
}
