package com.techq.available;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public class AvailableConfig {
	public final static int syncTick = 2;
	public final static int tickTime = 1000;
	
	public final static int connectedTick = 5;
	
	
	
	/**
	 * for follower
	 */
	public final static int pollTimeout = 200;
	public final static TimeUnit pollTimeUnit = TimeUnit.MILLISECONDS;
	public final static int wakeUpTimeout = 200;
	public final static int followTimeOut = 1000;
	public final static int followFailedCntLimit = 1;
	
	
	
	
	
	/**
	 * ping port
	 */
	public final static int HEARTBEAT_PORT = 7919;
	public final static String HEARTBEAT_IP = "127.0.0.1";
	
	
	
	/**
	 * client port
	 */
	public final static int CLIENT_PORT = 5551;
	public final static String CLIENT_SERVING_IP = "127.0.0.1";
	
	
	/**
	 * for debug client port
	 */
	public final static int CLIENT_PORT_1 = 5551;
	public final static int CLIENT_PORT_2 = 5552;
	public final static int CLIENT_PORT_3 = 5552;
	
	public final static Map<Long, Integer> clientPorts;
	static {
		clientPorts = new HashMap<Long, Integer>();
		clientPorts.put(1L, CLIENT_PORT_1);
		clientPorts.put(2L, CLIENT_PORT_2);
		clientPorts.put(3L, CLIENT_PORT_3);
	}
	
	
	public final static int MAX_CLIENT_CNXN = 60;
	
	
}
