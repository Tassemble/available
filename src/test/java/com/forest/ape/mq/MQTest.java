package com.forest.ape.mq;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.forest.ape.mq.impl.SendWorkerMQ;

public class MQTest {

	@Test
	public void testSendWorkerMQ() throws Exception {
		SendWorkerMQ worker = SendWorkerMQ.createMQWorker();

		byte b[] = new byte[4];
		b[0] = (byte) 2;
		b[1] = (byte) 0;
		b[2] = (byte) 1;
		b[3] = (byte) 2;
		ByteBuffer buf = ByteBuffer.wrap(b);
		buf.position(b.length);
		// Integer.toHexString(i)
		System.out.println("postion:" + buf.position() + ", limit:" + buf.limit());
		System.out.println("send:0x" + hexDump(buf, 0, buf.position()));

		worker.publish(buf);

	}

	public static String hexDump(ByteBuffer buffer, int from, int length) {
		if (length < 0) {
			throw new IllegalArgumentException("length: " + length);
		}
		if (length == 0) {
			return "";
		}

		int endIndex = from + length;
		char[] buf = new char[length << 1];

		int srcIdx = from;
		int dstIdx = 0;
		for (; srcIdx < endIndex; srcIdx++, dstIdx += 2) {
			System.arraycopy(HEXDUMP_TABLE,
					getUnsignedByte(buffer, srcIdx) << 1, buf, dstIdx, 2);
		}

		return new String(buf);
	}

	private static final char[] HEXDUMP_TABLE = new char[256 * 4];

	static {
		final char[] DIGITS = "0123456789abcdef".toCharArray();
		for (int i = 0; i < 256; i++) {
			HEXDUMP_TABLE[(i << 1) + 0] = DIGITS[i >>> 4 & 0x0F];
			HEXDUMP_TABLE[(i << 1) + 1] = DIGITS[i >>> 0 & 0x0F];
		}
	}

	public static short getUnsignedByte(ByteBuffer buf, int index) {
		return (short) (buf.get(index) & 0xFF);
	}

}
