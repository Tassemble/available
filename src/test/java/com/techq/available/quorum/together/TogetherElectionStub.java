package com.techq.available.quorum.together;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techq.available.quorum.Election;
import com.techq.available.quorum.ElectionStub;
import com.techq.available.quorum.Message;
import com.techq.available.quorum.Notification;
import com.techq.available.quorum.ServerState;
import com.techq.available.quorum.Vote;

public class TogetherElectionStub implements Election {
	private static final Logger LOG = LoggerFactory.getLogger(ElectionStub.class);
	final LinkedBlockingQueue<Notification> sendqueue = new LinkedBlockingQueue<Notification>();
	final LinkedBlockingQueue<Notification> recvqueue = new LinkedBlockingQueue<Notification>();
	
	@Override
	public long whoIsLeader() {
		return 0;
	}

	@Override
	public Vote lookForLeader() throws InterruptedException {
		return null;
	}

	@Override
	public Notification pollConfirm(long timeout, TimeUnit unit)
			throws InterruptedException {
		return recvqueue.poll(timeout, unit);
	}

	@Override
	public void pushback(Notification n) throws InterruptedException {

	}

	@Override
	public void offerACK(Notification n) {
		sendqueue.add(n);
	}

	@Override
	public Notification pollPing(long timeout, TimeUnit unit)
			throws InterruptedException {
		return recvqueue.poll(timeout, unit);
	}

	@Override
	public void offerPING(Notification n) throws InterruptedException {
		sendqueue.add(n);
	}

	@Override
	public void offerAgree(Notification n) {
		sendqueue.add(n);
	}
	
	public void init(int param) {
	
		final DataOutputStream dos;
		final DataInputStream dis;
		try {
			if (param == 1) {
				Socket socket = new Socket("127.0.0.1", 4444);
				dos = new DataOutputStream(socket.getOutputStream());
				dis = new DataInputStream(socket.getInputStream());
			} else {
				ServerSocket serverSocket = new ServerSocket(4444);
				LOG.debug("wait the client...");
				Socket socket = serverSocket.accept();
				dos = new DataOutputStream(socket.getOutputStream());
				dis = new DataInputStream(socket.getInputStream());
			}
			Thread outWorker = new Thread() {
				public void run(){
					while(true) {
						try {
							int len = 2 * 4 + 5 * 8;
							Notification m = sendqueue.poll(200, TimeUnit.MILLISECONDS);
							if (m == null)
								continue;
							LOG.info("send:" + m);
							byte requestBytes[] = new byte[len];
							ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);
							requestBuffer.clear();
							requestBuffer.putInt(m.getType().ordinal());
							requestBuffer.putInt(m.getState().ordinal());
							requestBuffer.putLong(m.getLeader());
							requestBuffer.putLong(m.getZxid());
							requestBuffer.putLong(m.getLogicalClock());
							requestBuffer.putLong(m.getFrom());//from which peer
							requestBuffer.putLong(m.getSid());//from which peer
							try {
								dos.write(requestBytes);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			};
			Thread inWorker = new Thread() {
				public void run(){
					while(true) {
							int len = 2 * 4 + 5 * 8;
							byte requestBytes[] = new byte[len];
							try {
								dis.read(requestBytes);
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							Notification n = new Notification();
							ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);
							int type = requestBuffer.getInt();
							switch (type) {
							case 3:
								n.setType(Notification.mType.AGREEMENT);
								break;
							case 5:
								n.setType(Notification.mType.PING);
								break;
							case 6:
								n.setType(Notification.mType.ACK);
								break;
							case 7:
								n.setType(Notification.mType.CONFIRM);
								break;
							default:
								LOG.warn("what is this? type is " + type);
								break;
							}
							// server state
							long state = requestBuffer.getInt();
							long leader = requestBuffer.getLong();
							long zxid = requestBuffer.getLong();
							long clock = requestBuffer.getLong();
							long from = requestBuffer.getLong();
							long sid = requestBuffer.getLong();
							n.setState(ServerState.FOLLOWING);
							n.setLeader(leader);
							n.setZxid(zxid);
							n.setLogicalClock(clock);
							n.setFrom(from);
							n.setSid(sid);
							recvqueue.offer(n);
					}
				}
			};
			outWorker.start();
			inWorker.start();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public static void main1(String[] args) {

		Thread sendWork = new Thread() {
			final TogetherElectionStub stub = new TogetherElectionStub();
			public void run() {
				//server
				stub.init(0);
				Notification n = new Notification(Notification.mType.AGREEMENT, 1, 2, 3, ServerState.FOLLOWING, 1, 1);
				LOG.info("send not:" + n);
				stub.offerAgree(n);
			}
		};
		Thread recvWork = new Thread() {
			final TogetherElectionStub stub = new TogetherElectionStub();
			public void run() {
				//client
				stub.init(1);
				try {
					Notification n = stub.pollConfirm(200, TimeUnit.MILLISECONDS);
					LOG.info("recv:" + n);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		sendWork.start();
		recvWork.start();
		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LOG.info("end!");
	}
	
	public static void main(String[] args) {
		
	}
	

}
