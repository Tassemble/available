package com.forest.ape.mq.impl;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.mq.CallableHandler;

public class DefaultRecvCallableHandler implements CallableHandler {
	static Logger LOG = LoggerFactory.getLogger(DefaultRecvCallableHandler.class);
	
	@Override
	public boolean handleRecv(byte[] data, Object object) {
		try {
			LOG.debug(new String(data, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return true;
	}

}
