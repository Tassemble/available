package com.forest.ape.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.server.ApeServer;
import com.forest.ape.server.Server;

public class NettyServerCnxn extends ServerCnxn {

	Logger LOG = LoggerFactory.getLogger(NettyServerCnxn.class);
	Channel channel;
	Server server;
	NettyServerCnxnFactory nettyServerCnxnFactory;
	long sessionId;
	
	
	
	public long getSessionId() {
		return sessionId;
	}


	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}



	
	public NettyServerCnxn(Channel channel, Server s,
			NettyServerCnxnFactory nettyServerCnxnFactory) {
		this.channel = channel;
		this.nettyServerCnxnFactory = nettyServerCnxnFactory;
		this.server = s;
	}



	ByteBuffer bb = null;
	int Default_Size = 100;
	int INT_SIZE = 4;
	int readIndex = 0;
	boolean isReadingLen = false;
	public void receiveMessage(ChannelBuffer channelBuffer) throws IOException {
		
		LOG.debug("process message..., buffer length:" + channelBuffer.readableBytes());
		try {
			while (channelBuffer.readable()) {
				if (bb != null) {
					bb.limit(bb.capacity());
					if (bb.remaining() <= channelBuffer.readableBytes()) {
//						LOG.debug("read byte len:" + (bb.limit() - bb.position()) + ", postion:" + bb.position() + ", limit:" + bb.limit());
						LOG.debug("read index:" + readIndex + ", read len:" + bb.remaining());
						readIndex += bb.remaining();
						channelBuffer.readBytes(bb);
						if (isReadingLen == false) {
							server.processPacket(this, bb);
							bb = null;
							increasePacketReceived();
						} else {
							bb.position(0);
							int dataLen = bb.getInt();
							if (dataLen > 1024)
								throw new IOException("data length: " +dataLen+ "is too long!!");
							isReadingLen = false;
							LOG.debug("allocate length:" + dataLen);
							bb = ByteBuffer.allocate(dataLen);
							
//							ByteBuffer dup = bb.duplicate();
//							dup.flip();
//							LOG.trace("queuedBuffer 0x"
//	                                + ChannelBuffers.hexDump(ChannelBuffers.copiedBuffer(dup)));
						}
					} else {
						int len = channelBuffer.readableBytes();
						bb.limit(bb.position() + len);
						LOG.debug("read index:" + readIndex + ", read len:" + len);
						readIndex += len;
						channelBuffer.readBytes(bb);

					}
					
					
				} else {
//					int len = channelBuffer.readInt();
//					if (len < 4) {
//						throw new IOException("recv len:" + len);
//					}
//					LOG.debug("allocate length:" + len);
					
					bb = ByteBuffer.allocate(INT_SIZE);
					if (channelBuffer.readableBytes() >= INT_SIZE) {
						//
						int dataLen = channelBuffer.readInt();
						LOG.debug("read index:" + readIndex + ", read len:" + dataLen);
						readIndex += dataLen;
						if (dataLen > 1024)
							throw new IOException("data length: " +dataLen+ " is too long!!");
						bb = ByteBuffer.allocate(dataLen);
						isReadingLen = false;
					} else {
						bb.limit(channelBuffer.readableBytes());
						LOG.debug("read index:" + readIndex + ", read len:" + channelBuffer.readableBytes());
						readIndex += channelBuffer.readableBytes();
						channelBuffer.readBytes(bb);
						isReadingLen = true;
					}
				}
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			channel.close();
		}

		// apeServer.processPacket(cnxn, e.getMessage());
		
	}
	
	
	static AtomicLong cnt = new AtomicLong(0);
	private void increasePacketReceived() {
		// TODO Auto-generated method stub
		LOG.info("^_^ recv one packet! seq:" + cnt.addAndGet(1));
	}

	
	public void close() {
		nettyServerCnxnFactory.removeCnxn(this);
		if (channel.isOpen()) {
			LOG.debug("closing channel, ox" + Long.toHexString(sessionId));
			channel.close();
		}
	}

	public void sendMessage(ByteBuffer buf) {
		
	}
}
