/**
 * 
 */
package com.forest.ape.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.forest.ape.nio.ServerCnxnFactory;

/**
 * @author CHQ
 * 2012-2-8
 */
public class ApeApp {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		ServerCnxnFactory factory = ServerCnxnFactory.createFactory();
		factory.configure(new InetSocketAddress(9999), 100);
		factory.start();
	}

}
