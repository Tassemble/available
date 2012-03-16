package com.forest.ape.mq.impl;

import java.util.List;
import com.forest.ape.mq.CallableHandler;

public class WorkerFactory {
	
	RecvWorker rWorker = null;
	SendWorker sWorker = null;
	
	
	/**
	 * MQ want to tell who is leader(id) and followers,
	 * if i am leader, i must take responsibility to create sending queues, but if i am not leader, 
	 * i just need to create my receiving queue
	 * @param leaderId
	 * @param followers
	 */
	@Deprecated
	private WorkerFactory(String leaderId, List<String> followers, String epoch, String myId){
		sWorker = new SendWorker(leaderId, followers, epoch);
	}
	
	
	private WorkerFactory(RecvWorker r, SendWorker s) {
		rWorker = r;
		sWorker = s;
	}
	
	WorkerFactory() {}
	
	
	/**
	 * if i am leader, i must take responsibility to create sending queues
	 * @param leaderId
	 * @param followers
	 * @param epoch
	 * @return
	 */
	public WorkerFactory buildByLeader(String leaderId, List<String> followers, String epoch) {
		sWorker = new SendWorker(leaderId, followers, epoch);
		return new WorkerFactory(null, sWorker);
	}
	
	
	/**
	 * if i am not leader, i just need to create my receiving queue
	 * @param followerId
	 * @param epoch
	 * @return
	 */
	public WorkerFactory buildByFollower(String followerId,  String epoch) {
		rWorker = new RecvWorker(followerId, epoch);
		return new WorkerFactory(rWorker, null);
	}
	
	
	/**
	 * if i am not leader, i just need to create my receiving queue
	 * @param followerId
	 * @param epoch
	 * @return
	 */
	public WorkerFactory buildByFollower(String followerId,  String epoch, CallableHandler handler) {
		rWorker = new RecvWorker(followerId, epoch);
		rWorker.setCallHandler(handler);
		return new WorkerFactory(rWorker, null);
	}
	
	
	/**
	 * leader dont consider anything about how msg sent and copes with it, 
	 * but may be it wants to know whether it is sent
	 * @param data
	 * @param handler
	 */
	@Deprecated
	public void enQueue(byte[] data, CallableHandler handler) {
		throw new UnsupportedOperationException("you haven't implemented");
	}
	
	
	/**
	 * leader dont consider anything about how msg sent and copes with it, 
	 * but may be it wants to know whether it is sent
	 * @param data
	 * @param handler
	 */
	public boolean enQueue(MQPacket packet) {
		return sWorker.enQueue(packet);
	}
	
	
	/**
	 * leader dont consider anything about how msg sent and copes with it, 
	 * @param data
	 * @param handler
	 */
	@Deprecated
	public void enQueue(byte[] data) {
		if (sWorker == null) {
			throw new UnsupportedOperationException("you are not leader, don't do sending operation");
		}
	}
	
	
	/**
	 * whatever role of learner,  it should cope with the msg, must set it
	 * @param handler
	 */
	public void recvQueue(CallableHandler handler) {
		rWorker.setCallHandler(handler);
	}
	
	
	
	
	public void start() {
		if (sWorker != null)
			sWorker.start();
		if (rWorker != null)
			rWorker.start();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
