/**
 * 
 */
package com.forest.ape;

import java.io.IOException;

import com.forest.ape.mq.impl.ConsumerGaurd;
import com.forest.ape.mq.impl.DefaultRecvHandler;

/**
 * @author CHQ
 * 2012-3-23
 */
public class ConsumerMain {
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		ConsumerGaurd gaurd = new ConsumerGaurd(new DefaultRecvHandler());
		gaurd.start();
	}
	
	
	

}
