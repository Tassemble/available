package com.forest.ape.server.persistence;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.forest.ape.data.ACL;
import com.forest.ape.data.Id;
import com.forest.ape.server.DataTree;
import com.forest.ape.server.ZooDefs.OpCode;
import com.forest.ape.server.ZooDefs.Perms;
import com.forest.ape.txn.CreateTxn;
import com.forest.ape.txn.Txn;
import com.forest.ape.txn.TxnHeader;

public class TxnLogTest {

	@Test
	public void testFileTxnLog() throws IOException {
//		TxnLog log = new FileTxnLog(new File("logDir"));
		long clientId = 1L; int cxid= 1; long zxid= 1L; long time = System.currentTimeMillis(); int type = 1;
//		log.append(new TxnHeader(clientId, cxid, zxid, time, type), null);
		FileTxnSnapLog helper =  new FileTxnSnapLog(new File("DataLog"), new File("LogDir"));
		List<Id> ids = new ArrayList<Id>();
		ids.add(new Id("ip", "hahaha"));
		ByteBuffer buf = ByteBuffer.allocate(100);
		buf.put(new String("data from chq").getBytes());
		buf.flip();
		Request r = new Request(null, 1, 1, 1, buf, ids);
		List<ACL> acls = new ArrayList<ACL>();
		acls.add(new ACL(Perms.ALL, new Id("ip", "192.168.17.1")));
		r.hdr = new TxnHeader(clientId, cxid, zxid, time, OpCode.create);
		r.txn = new CreateTxn("/abc", "chq".getBytes(), acls, true, 0);
		//System.out.println(r.hdr + ", " + r.txn);
		boolean isOK = helper.append(r);
		System.out.println(isOK);
		helper.commit();
		
		r.hdr = new TxnHeader(clientId, cxid, zxid + 1, time, OpCode.create);
		r.txn = new CreateTxn("/abc/aa", "chq".getBytes(), acls, true, 0);
		helper.append(r);
		helper.commit();
		
		
		r.hdr = new TxnHeader(clientId, cxid, zxid + 2, time, OpCode.create);
		r.txn = new CreateTxn("/abc/aa/dd", "chq".getBytes(), acls, true, 0);
		helper.append(r);
		helper.commit();
		
//		
		DataTree dt = new DataTree();
		Map<Long, Integer> sessions = new HashMap<Long, Integer>();
		helper.restore(dt, sessions);
		System.out.println(dt.lastProcessedZxid);
		//long xid = helper.getLastLoggedZxid();
		//System.out.println(xid);
	}
}
