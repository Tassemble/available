package com.forest.ape.mq;

import java.util.Random;

import org.slf4j.*;

public class DataBridgeMock implements DataBridge {
	Logger LOG = LoggerFactory.getLogger(DataBridgeMock.class);
	Random r = new Random(System.currentTimeMillis());
	
	@Override
	public boolean append(Object o) {
		LOG.debug("append ok");
		return r.nextBoolean();
	}

	@Override
	public boolean commit() {
		LOG.debug("commit ok");
		return true;
	}

}
