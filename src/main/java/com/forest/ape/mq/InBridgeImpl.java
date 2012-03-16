package com.forest.ape.mq;

import com.forest.ape.server.persistence.FileTxnSnapLog;

public class InBridgeImpl implements InBridge {

	FileTxnSnapLog txn;
	
	
	public InBridgeImpl(FileTxnSnapLog txn) {
		super();
		this.txn = txn;
	}

	@Override
	public boolean append(Object o) {
		throw new UnsupportedOperationException("you haven't implemented");
	}

	@Override
	public boolean commit() {
		throw new UnsupportedOperationException();
	}

}
