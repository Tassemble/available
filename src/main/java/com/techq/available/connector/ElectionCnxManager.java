package com.techq.available.connector;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.techq.available.quorum.Message;

/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public interface ElectionCnxManager {

	
	public Message pollRecvQueue(long time, TimeUnit unit) throws InterruptedException;
	
	
	public void offerSendQueue(long sid, ByteBuffer buffer);
	
	
	public boolean haveDelivered();
	
	
	
	//public void connectOne(long sid);
	
	
	public void connectAll() ;
	
	
	public void toSend(Long sid, ByteBuffer b);
	
	
	public void startListen();
	
	
	public void halt();
	
	
	
	
	
	
}
