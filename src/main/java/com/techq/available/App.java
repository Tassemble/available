package com.techq.available;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techq.available.quorum.LeaderElection;
import com.techq.available.quorum.ProposalVote;
import com.techq.available.quorum.QuorumPeer;
import com.techq.available.quorum.ServerState;

/**
 * 
 * @author CHQ
 * 2012-2-3
 */
public class App 
{
	//static Logger LOG = Logger.getLogger(App.class);
	private static final Logger LOG = LoggerFactory.getLogger(App.class);

	static long myId = -1;
	static long numPeer = 3;
	static InetSocketAddress currentServerAddress = new InetSocketAddress(8888);
	static Map<Long, InetSocketAddress[]> servers = new HashMap<Long, InetSocketAddress[]>();
	static {
		// server id:1, 2, 3
//		for (long i = 0; i < numPeer; i++) {
//			long serverId = i + 1;
//			servers.put(serverId, new InetSocketAddress[] {
//					new InetSocketAddress("127.0.0.1"/* + String.valueOf(serverId)*/, 6555),
//					new InetSocketAddress("127.0.0.1"/* + String.valueOf(serverId)*/, 6556) 
//					});
//		}
		long serverId = 1;
		servers.put(serverId++, new InetSocketAddress[] {
				new InetSocketAddress("127.0.0.1"/* + String.valueOf(serverId)*/, 6555),
				new InetSocketAddress("127.0.0.1"/* + String.valueOf(serverId)*/, 6556) 
				});
		servers.put(serverId++, new InetSocketAddress[] {
				new InetSocketAddress("127.0.0.1"/* + String.valueOf(serverId)*/, 7555),
				new InetSocketAddress("127.0.0.1"/* + String.valueOf(serverId)*/, 7556) 
				});
		servers.put(serverId++, new InetSocketAddress[] {
				new InetSocketAddress("127.0.0.1"/* + String.valueOf(serverId)*/, 8555),
				new InetSocketAddress("127.0.0.1"/* + String.valueOf(serverId)*/, 8556) 
				});
	}
	
	
	/**
	 * @param args
	 *            void
	 * @throws IOException
	 */
	public QuorumPeer createPeer(long myId) throws IOException {
		QuorumPeer quorumPeer = null;
		LOG.info("Starting quorum peer");


		// 设置其他的peers(map<sid, peer>)
		// 在配置文件中有：那么这里的sid分别是server字段后面的1、2、3 Long.valueOf(sid)
		// server.1=zoo1:2888:3888
		// server.2=zoo2:2888:3888
		// server.3=zoo3:2888:3888
		// servers.put(Long.valueOf(sid), new QuorumServer(sid, addr));
		Map<Long, QuorumPeer.QuorumServer> peers = new HashMap<Long, QuorumPeer.QuorumServer>();

		/**
		 * serverId, 正常更新端口，选举端口，当然还有一个参数是表示类型（ PARTICIPANT, OBSERVER;）
		 */
		for (int i = 0; i < numPeer; i++) {
			int serverId = i + 1;
			// 数据传输端口
			InetSocketAddress addr1 = servers.get(new Long(serverId))[0];
			// 选举端口
			InetSocketAddress addr2 = servers.get(new Long(serverId))[1];
			QuorumPeer.QuorumServer server = new QuorumPeer.QuorumServer(new Long(serverId), addr1, addr2);
			peers.put(new Long(serverId), server);
		}
		quorumPeer = new QuorumPeer(peers, new ProposalVote(0, myId, 0, ServerState.LOOKING), myId);


		quorumPeer.setTickTime(2000);

		/**
		 * 
		 * initLimit is timeouts ZooKeeper uses to limit the length of time the
		 * ZooKeeper servers in quorum have to connect to a leader. time = 5 *
		 * tick = 5 * 2000 = 10s
		 */
		quorumPeer.setInitLimit(5);

		quorumPeer.setSyncLimit(2);

		return quorumPeer;
	}
	
	
    public static void main( String[] args ) throws IOException, InterruptedException
    {
    	QuorumPeer peer = new App().createPeer(1L);
    	peer.start();
    }
}
