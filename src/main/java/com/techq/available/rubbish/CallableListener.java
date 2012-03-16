package com.techq.available.rubbish;

public interface CallableListener {
	public void doit(Object o);
	
	public void failedDo(Object o);
	
	
	public void succeededDo(Object o);
}
