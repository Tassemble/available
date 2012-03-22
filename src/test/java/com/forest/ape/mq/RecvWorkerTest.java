package com.forest.ape.mq;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.*;

import com.forest.ape.mq.impl.RecvWorker;
import com.rabbitmq.client.*;

public class RecvWorkerTest {
	Logger LOG = LoggerFactory.getLogger(RecvWorkerTest.class);
		
	
	@Ignore
	@BeforeClass
	public static void setup() {
		PropertyConfigurator.configure("conf/log4j.properties");
	}
	
	@Ignore
	@Test
	public void recvWorkerTest() throws InterruptedException, UnsupportedEncodingException, FileNotFoundException, IOException {
		Properties properties = new Properties();
		properties.load(new InputStreamReader(new FileInputStream(
			"conf/config"), "UTF-8"));
		String values = (String) properties.get("MQClusterAddrs");
		List<Address> adds = new ArrayList<Address>();
		
		for (String value: values.split(";")) {
			String[] fields = value.split(":");
			adds.add(new Address(fields[0], Integer.valueOf(fields[1])));
		}
		Address[] addrArr = new Address[adds.size()];
		int cnt = 0;
		for (Address address : adds) {
			addrArr[cnt++] = new Address(adds.get(0).getHost(), adds.get(0).getPort());
		}
		
		values = (String) properties.get("MQCluster-ha-policy");
	
	}
}
