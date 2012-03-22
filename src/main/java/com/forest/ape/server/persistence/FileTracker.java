/**
 * 
 */
package com.forest.ape.server.persistence;

import java.io.IOException;
import java.io.InputStream;

import com.forest.ape.exception.ApeException.NoNodeException;
import com.forest.ape.exception.ApeException.NodeExistsException;

/**
 * @author CHQ 2012-3-19
 */
public interface FileTracker {

	interface FileHandler {
		public boolean appendData(byte[] data);

		public boolean deleteData();

		public InputStream readData() throws NoNodeException;
	}
	
	FileHandler createFileHandler(String path) throws NodeExistsException;
	
	
	void addFileHandler(String path, FileHandler h) throws NodeExistsException;
	
	
	void removeFileHandler(String path) throws NoNodeException;
	
	boolean existPath(String path) throws IOException;
	
	FileHandler getFileHandler(String path);
}
