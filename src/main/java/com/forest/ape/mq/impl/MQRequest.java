package com.forest.ape.mq.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.sun.istack.internal.Interned;

/**
 * 
 * @author CHQ
 * 2012-3-21
 */
public class MQRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int type;
	String path;
	byte[] value;
	
	
	public MQRequest(int type, String path, byte[] value) {
		super();
		this.type = type;
		this.path = path;
		this.value = value;
	}
	
	
	public static byte[] convert2Bytes(MQRequest r) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);   
		out.writeObject(r);
		return bos.toByteArray();
	}
	
	public static MQRequest convert2MQRequest(byte[] request) throws ClassNotFoundException, IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(request);
		ObjectInput in = new ObjectInputStream(bis);
		MQRequest o = (MQRequest)in.readObject();
		return o;
	}
	
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		MQRequest request = new MQRequest(1, "ahh", "heyhey".getBytes("UTF-8"));
		byte[] b = MQRequest.convert2Bytes(request);
		request = MQRequest.convert2MQRequest(b);
		System.out.println("path:" + request.path + ", type:" + request.type + ", value:" + new String(request.value, "UTF-8"));
	}
	
	
	
	
	
}
