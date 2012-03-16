package com.forest.ape.jmx;


/**
 * This MBean represents a client connection.
 */
public interface ConnectionMXBean {
    /**
     * @return source (client) IP address
     */
    public String getSourceIP();
    /**
     * @return client's session id
     */
    public String getSessionId();
    /**
     * @return time the connection was started
     */
    public String getStartedTime();
    
    /**
     * @return packets received from this client
     */
    public long getPacketsReceived();
    
    /**
     * @return number of packets sent to this client
     */
    public long getPacketsSent();
    
    /**
     * @return number of requets being processed
     */
    public long getOutstandingRequests();
    
    /**
     * @return session timeout in ms
     */
    public int getSessionTimeout();
    
    /**
     * Terminate this client session. The client will reconnect with a different
     * session id.
     */
    public void terminateSession();
    /**
     * Terminate thei client connection. The client will immediately attempt to 
     * reconnect with the same session id.
     */
    public void terminateConnection();


    /** Min latency in ms
     * @since 3.3.0 */
    long getMinLatency();
    /** Average latency in ms
     * @since 3.3.0 */
    long getAvgLatency();
    /** Max latency in ms
     * @since 3.3.0 */
    long getMaxLatency();
    /** Last operation performed by this connection
     * @since 3.3.0 */
    String getLastOperation();
    /** Last cxid of this connection
     * @since 3.3.0 */
    String getLastCxid();
    /** Last zxid of this connection
     * @since 3.3.0 */
    String getLastZxid();
    /** Last time server sent a response to client on this connection
     * @since 3.3.0 */
    String getLastResponseTime();
    /** Latency of last response to client on this connection in ms
     * @since 3.3.0 */
    long getLastLatency();

    /** Reset counters
     * @since 3.3.0 */
    void resetCounters();
}
