package com.forest.ape.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

import javax.management.JMException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.jmx.ConnectionBean;
import com.forest.ape.jmx.MBeanRegistry;
import com.forest.ape.server.ApeServer;
import com.forest.ape.server.Server;


public abstract class ServerCnxnFactory {
	  
	Logger LOG = LoggerFactory.getLogger(ServerCnxnFactory.class);
	Server server;
	
	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public abstract void join() throws InterruptedException;

    public abstract void shutdown();

    public abstract void start();
    
    public abstract void configure(InetSocketAddress addr, int maxClientCnxns) 
    throws IOException;
    
    
    /** Maximum number of connections allowed from particular host (ip) */
    public abstract int getMaxClientCnxnsPerHost();

    /** Maximum number of connections allowed from particular host (ip) */
    public abstract void setMaxClientCnxnsPerHost(int max);
    
    public abstract void closeAll();
    
    public abstract InetSocketAddress getLocalAddress();

    private HashMap<NettyServerCnxn, ConnectionBean> connectionBeans = new HashMap<NettyServerCnxn, ConnectionBean>();
    
    public void unregisterConnection(NettyServerCnxn serverCnxn) {
        ConnectionBean jmxConnectionBean = connectionBeans.remove(serverCnxn);
        if (jmxConnectionBean != null){
            MBeanRegistry.getInstance().unregister(jmxConnectionBean);
        }
    }
    
    public void registerConnection(NettyServerCnxn serverCnxn) {
        /*if (zkServer != null) {
            ConnectionBean jmxConnectionBean = new ConnectionBean(serverCnxn, zkServer);
            try {
                MBeanRegistry.getInstance().register(jmxConnectionBean, zkServer.jmxServerBean);
                connectionBeans.put(serverCnxn, jmxConnectionBean);
            } catch (JMException e) {
                LOG.warn("Could not register connection", e);
            }
        }*/

    }
    
    static public ServerCnxnFactory createFactory() throws IOException {
        String serverCnxnFactoryName = NettyServerCnxnFactory.class.getName();
        try {
            return (ServerCnxnFactory) Class.forName(serverCnxnFactoryName)
                                                .newInstance();
        } catch (Exception e) {
            IOException ioe = new IOException("Couldn't instantiate "
                    + serverCnxnFactoryName);
            ioe.initCause(e);
            throw ioe;
        }
    }
    
    
}
