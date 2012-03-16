package com.techq.available.connector;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import com.techq.available.quorum.Message;

/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public interface LearnerCnxManager {

	public Message pollRecvQueue(long time, TimeUnit unit) throws InterruptedException;
	
	public void toSend(Long sid, ByteBuffer b);
	
	
}
