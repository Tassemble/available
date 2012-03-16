package com.forest.ape.mq;

public interface InBridge {

	public boolean append(Object o);
	
	
	public boolean commit();
	
	
	
}
