package com.forest.ape.mq;


/**
 * 
 * @author CHQ
 * 2012-3-16
 */
public interface CallableHandler {
	
	interface AsynSentHandler {	
		public void handleAck(boolean isOK);
	}
	
	
	public boolean handleRecv(byte[] data, Object object);
	
}
