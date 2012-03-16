package com.forest.ape.mq.impl;

import com.forest.ape.mq.CallableHandler;
import com.forest.ape.mq.CallableHandler.AsynSentHandler;

public class MQPacket {
	byte[] data;
	CallableHandler.AsynSentHandler handler;
	
	
	
	
	public MQPacket(byte[] data, AsynSentHandler handler) {
		super();
		this.data = data;
		this.handler = handler;
	}
	public CallableHandler.AsynSentHandler getHandler() {
		return handler;
	}
	public void setHandler(CallableHandler.AsynSentHandler handler) {
		this.handler = handler;
	}
	
}
