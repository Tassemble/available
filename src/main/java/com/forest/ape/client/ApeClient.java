package com.forest.ape.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.apache.jute.BinaryOutputArchive;

import com.forest.ape.server.Type;
import com.techq.available.data.BasicPacket;

public class ApeClient {

	static int CLIENT_PORT = 9876;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Socket socket = new Socket("127.0.0.1", ApeClient.CLIENT_PORT);
			BinaryOutputArchive oa = null;
			oa = BinaryOutputArchive.getArchive(socket.getOutputStream());
			while (true) {
				BasicPacket packet = new BasicPacket();
				packet.setType(Type.DATA);
				String msg = "Hello, MQ!";
				packet.setData(msg.getBytes());
				oa.writeRecord(packet, "data");
				TimeUnit.SECONDS.sleep(1);
			}
		} catch (IOException e1) {
			e1.printStackTrace(System.out);
			return; 
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void old() {
		try {
			Socket socket = new Socket("127.0.0.1", ApeClient.CLIENT_PORT);
			DataOutputStream out = new DataOutputStream(
					socket.getOutputStream());

			while (true) {
				String msg = "Hello, MQ!";
				TimeUnit.MILLISECONDS.sleep(100);
				out.write(msg.getBytes());
			}

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
