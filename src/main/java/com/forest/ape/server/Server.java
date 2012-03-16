package com.forest.ape.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.forest.ape.nio.ServerCnxn;

public abstract class Server {

	public abstract void processPacket(ServerCnxn cnx, ByteBuffer buf) throws IOException;

	public abstract void shutdown();
	
	
}
