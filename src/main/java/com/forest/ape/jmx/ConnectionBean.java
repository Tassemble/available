package com.forest.ape.jmx;

/**
 * 
 * @author CHQ
 * 2012-2-6
 */
public class ConnectionBean implements ConnectionMXBean, ApeMBeanInfo {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isHidden() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSourceIP() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStartedTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getPacketsReceived() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getPacketsSent() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getOutstandingRequests() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSessionTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void terminateSession() {
		// TODO Auto-generated method stub

	}

	@Override
	public void terminateConnection() {
		// TODO Auto-generated method stub

	}

	@Override
	public long getMinLatency() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getAvgLatency() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getMaxLatency() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getLastOperation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLastCxid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLastZxid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLastResponseTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getLastLatency() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void resetCounters() {
		// TODO Auto-generated method stub

	}

}
