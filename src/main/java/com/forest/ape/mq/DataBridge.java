package com.forest.ape.mq;

public interface DataBridge {

	public boolean append(Object o);
	
	
	public boolean commit();
	
	
	
}
