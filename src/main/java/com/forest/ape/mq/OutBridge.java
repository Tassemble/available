package com.forest.ape.mq;

import java.io.IOException;

import com.forest.ape.mq.impl.MQPacket;
import com.rabbitmq.client.*;

/**
 * 
 * @author CHQ
 * 2012-3-16
 */
public interface OutBridge {

	public void enmq(Channel ch, MQPacket re);
	public void enmq(String exchange, Channel ch, MQPacket re) throws IOException;
}
