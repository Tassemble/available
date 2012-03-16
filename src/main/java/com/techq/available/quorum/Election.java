package com.techq.available.quorum;

import java.util.concurrent.TimeUnit;

/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public interface Election {
	public long whoIsLeader();
	public Vote lookForLeader() throws InterruptedException;
	public Notification pollConfirm(long timeout, TimeUnit unit) throws InterruptedException;
	public void pushback(Notification n) throws InterruptedException;
	public void offerACK(Notification n);
	public Notification pollPing(long timeout, TimeUnit unit) throws InterruptedException;
	public void offerPING(Notification n)throws InterruptedException;
	
	
	public void offerAgree(Notification n);
}
