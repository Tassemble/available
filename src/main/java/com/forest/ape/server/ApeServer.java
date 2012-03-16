package com.forest.ape.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.forest.ape.mq.SendWorkerMQ;
import com.forest.ape.nio.ServerCnxn;
import com.forest.ape.server.persistence.Request;
import com.techq.available.data.BasicPacket;
import com.techq.available.quorum.handler.Leader;
import com.techq.available.quorum.handler.Learner;

/**
 * Name it Ape, sound funny!
 * 
 * @author CHQ 2012-2-6
 */
public class ApeServer extends Server {

	SendWorkerMQ mq;
	Learner peer;
	DataTree dt;

	public ApeServer(Learner learner) throws Exception {
		this.peer = learner;
		if (this.peer.isLeader())
			mq = SendWorkerMQ.createMQWorker();
		else 
			mq = null;
	}

	/**
	 * 
	 * @param cnx
	 * @param buf
	 * @throws IOException
	 */
	public void processPacket(ServerCnxn cnx, ByteBuffer buf)
			throws IOException {
		BasicPacket packet = new BasicPacket();
		packet.setType(Type.DATA);
		packet.setFrom(peer.getId());
		packet.setData(buf.array());
		peer.addPackets(packet);
	}

	public void shutdown() {

	}
	
    /**
     * send a request packet to the leader
     *
     * @param request
     *                the request from the client
     * @throws IOException
     */
    void request(com.forest.ape.server.persistence.Request request) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream oa = new DataOutputStream(baos);
        oa.writeLong(request.sessionId);
        oa.writeInt(request.cxid);
        oa.writeInt(request.type);
        if (request.request != null) {
            request.request.rewind();
            int len = request.request.remaining();
            byte b[] = new byte[len];
            request.request.get(b);
            request.request.rewind();
            oa.write(b);
        }
        oa.close();
        BasicPacket qp = new BasicPacket(Type.DATA, -1, peer.getId(), baos.toByteArray());
        writePacket(qp, true);
//        BasicPacket qp = new BasicPacket(Leader.REQUEST, -1, baos
//                .toByteArray(), request.authInfo);
//      
    }

	private void writePacket(BasicPacket qp, boolean b) {
		peer.addPackets(qp);
	}
	
	
	public void append(Request r) {
		
	}
	
	
	public void commit() {
		
	}
	
	
	
	
	

}
