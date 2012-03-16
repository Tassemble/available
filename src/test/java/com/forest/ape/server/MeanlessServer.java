package com.forest.ape.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import com.forest.ape.nio.NettyServerCnxnFactory;
import com.forest.ape.nio.ServerCnxn;

public class MeanlessServer extends Server {

	Logger LOG = LoggerFactory.getLogger(MeanlessServer.class);
	
	@Override
	public void processPacket(ServerCnxn cnx, ByteBuffer buf)
			throws IOException {
		LOG.debug("yes, i am busy processing data");
	}


	@Override
	public void shutdown() {
		LOG.debug("ok, i shutdown my self");
	}

}
