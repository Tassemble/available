package com.forest.ape.server.nio;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

public class TestFactoryClient {

	@Ignore
	@Test
	public void testSendMsg() throws UnknownHostException, IOException, InterruptedException {
		Socket socket = new Socket("127.0.0.1", 5551);
		OutputStream os = socket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(os);
//		dos.write("hello".length());
		for (int i = 0; i < 10; i++) {
			dos.writeInt("hah".length());
			dos.write("hah".getBytes());
		}
//		writer.write("hello".getBytes());
//		OutputStreamWriter writer = new OutputStreamWriter(dos);
//		writer.flush();
		TimeUnit.SECONDS.sleep(50);
	}
	
}
