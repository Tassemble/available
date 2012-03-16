package com.techq.available.connector;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techq.available.App;
import com.techq.available.connector.impl.ElectionCnxManagerImpl;
import com.techq.available.quorum.Message;
import com.techq.available.quorum.Notification;
import com.techq.available.quorum.QuorumPeer;
import com.techq.available.quorum.ServerState;

public class ConnectorTest {
	private static final Logger LOG = LoggerFactory
			.getLogger(ConnectorTest.class);

	public static ElectionCnxManager initConnector(long id) {

		try {
			QuorumPeer peer = new App().createPeer(id);
			ElectionCnxManager manager = new ElectionCnxManagerImpl(peer);
			manager.startListen();
			// manager.toSend(1L, )
			return manager;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) throws InterruptedException {
		// one send
		// one recv

		// to 2
		Thread sendThread = new Thread() {
			final ElectionCnxManager manager = initConnector(1);
			Notification n = new Notification(Notification.mType.AGREEMENT, 2,
					0, 2, ServerState.FOLLOWING, 2,// to whom
					1//
			);

			public void run() {
				try {
					while (true) {
						TimeUnit.SECONDS.sleep(1);
						LOG.info("send:" + n);
						ByteBuffer buffer = process(n);
						manager.toSend(2L, buffer);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(System.out);
				}
			}
		};

		Thread recvThread = new Thread() {
			final ElectionCnxManager manager = initConnector(2);
			public void run() {
				try {
					while (true) {
						Message response = manager.pollRecvQueue(200,
							TimeUnit.MILLISECONDS);
						if (response == null)
							continue;
						
						ByteBuffer buffer = response.getBuffer();
						int val = buffer.getInt();//4
						Notification.mType type = Notification.getTypeByInt(val);
						long state = response.getBuffer().getInt();//4
						long leader = response.getBuffer().getLong();//8
						long zxid = response.getBuffer().getLong();//8
						long electionEpoch = response.getBuffer().getLong();//8
						long from = response.getBuffer().getLong();//8
						long sid = 1;
						Notification n = new Notification(
								type,
								leader,
								zxid,
								electionEpoch,
								ServerState.FOLLOWING,
								from,//to whom
								sid//
								);
						LOG.info("recv:" + n);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		sendThread.start();
		recvThread.start();
		
		
		Thread.currentThread().join();

	}

	private static ByteBuffer process(Notification m) {
		byte requestBytes[] = new byte[Message.DEFAULT_SIZE];
		ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);

		/*
		 * Building notification packet to send
		 */

		/**
		 * int 4byte long 4 * 8 byte total bytes: 36bytes
		 */
		requestBuffer.clear();
		LOG.info("type:" + m.getType().ordinal());
		requestBuffer.putInt(m.getType().ordinal());
		requestBuffer.putInt(m.getState().ordinal());
		requestBuffer.putLong(m.getLeader());
		requestBuffer.putLong(m.getZxid());
		requestBuffer.putLong(m.getLogicalClock());
		requestBuffer.putLong(m.getFrom());// from which peer
		return requestBuffer;
	}

}
