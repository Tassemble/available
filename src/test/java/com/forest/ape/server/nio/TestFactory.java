package com.forest.ape.server.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import com.forest.ape.nio.ServerCnxnFactory;
import com.forest.ape.server.MeanlessServer;

public class TestFactory {
	
	
	@Ignore
	@Test
	public void testServerCnxnFactory() throws IOException, InterruptedException {
		ServerCnxnFactory factory = ServerCnxnFactory.createFactory();
		factory.configure(new InetSocketAddress(1111), 100);
		factory.setServer(new MeanlessServer());
		factory.start();

		System.out.println("i will shutdown it after 5 secs");
		TimeUnit.SECONDS.sleep(5);
		
		factory.shutdown();
		
		//factory.join();
		System.out.println("done!");
	}
}
