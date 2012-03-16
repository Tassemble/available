package com.forest.ape.mq;

import com.forest.ape.server.persistence.Request;
import com.rabbitmq.client.*;

public class OutBridgeImpl implements OutBridge {

	
	@Override
	public void enmq(Channel ch, Request re) {
		throw new UnsupportedOperationException();
	}
}
