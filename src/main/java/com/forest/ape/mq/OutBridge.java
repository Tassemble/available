package com.forest.ape.mq;

import com.forest.ape.server.persistence.Request;
import com.rabbitmq.client.*;

public interface OutBridge {

	
	public void enmq(Channel ch, Request re);
	
	
	
}
