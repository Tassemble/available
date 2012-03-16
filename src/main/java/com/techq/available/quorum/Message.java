package com.techq.available.quorum;

import java.nio.ByteBuffer;

/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public class Message {
	public static int DEFAULT_SIZE = 40;
	
	
	public static int DEBUG_DEFAULT_SIZE = 44;
	
	/**
	 * 0.type(int) 1. state(int), 2. leader(long) 3. zxid(long) 4. electionEpoch(long) 5. from(long)
	 * 2* 4 + 4 * 8 = 40 bytes
	 * @param buffer
	 * @param sid
	 */
	
	public Message(ByteBuffer buffer, long sid) {
		this.buffer = buffer;
		this.sid = sid;
	}
	ByteBuffer buffer;
	long sid;


	public ByteBuffer getBuffer() {
		return buffer;
	}
	public void setBuffer(ByteBuffer buffer) {
		this.buffer = buffer;
	}
	public long getSid() {
		return sid;
	}
	public void setSid(long sid) {
		this.sid = sid;
	}
	
	
	
	
	
}