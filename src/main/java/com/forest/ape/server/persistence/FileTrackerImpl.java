/**
 * 
 */
package com.forest.ape.server.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.exception.ApeException;
import com.forest.ape.exception.ApeException.NoNodeException;
import com.forest.ape.exception.ApeException.NodeExistsException;

/**
 * @author CHQ
 * 2012-3-19
 */
public class FileTrackerImpl implements FileTracker {
	private static final Logger LOG = LoggerFactory.getLogger(FileTrackerImpl.class);
	int initialCapacity = 1000;
	static String rootDir = ".";
	ConcurrentHashMap<String, FileTracker.FileHandler> fileHandlers 
		= new ConcurrentHashMap<String, FileTracker.FileHandler>(initialCapacity);
	static {
		loadConfig();
	}
	/**
	 * 
	 */
	public FileTrackerImpl() {}

	@Override
	public FileHandler createFileHandler(String path) throws NodeExistsException {
		return new ApeFileHandler(path);
	}
	
	
	@Override
	public void addFileHandler(String path, FileHandler h) throws NodeExistsException {
		if (fileHandlers.get(path) != null) {
			throw new ApeException.NodeExistsException("path exist:" + path);
		}
		fileHandlers.putIfAbsent(path, h);
	}
	
	@Override
	public void removeFileHandler(String path) throws NoNodeException {
		if (fileHandlers.remove(path) == null)
			throw new ApeException.NoNodeException(path);
	}
	
	
	
	public static class ApeFileHandler implements FileTracker.FileHandler {
		File dir;
		File file;
		final String path;
		

		public ApeFileHandler(String path) throws NodeExistsException {
			super();
			checkpath(path);
			this.path = rootDir + path;
			this.dir = new File(this.path);
			String filename = this.path.substring(this.path.lastIndexOf("/") + 1, this.path.length());
			try {
				dir.mkdirs();
				file = new File(this.path + "/." + filename);
				if (!file.exists()) {
					boolean res = file.createNewFile();
					if (res == true) {
						LOG.info("created file:" + this.path + "/." + filename);
					} else {
						throw new IOException("create file :" + this.path+ "/." + filename + " error" ) ;
					}
				} else {
					LOG.info("file exist:" + this.path + "." + filename);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				LOG.error("", e);
				throw new ApeException.NodeExistsException(this.path);
			}	
		}

		private void checkpath(String path) {}

		@Override
		public InputStream readData() throws NoNodeException {
			try {
				return new FileInputStream(new File(path));
			} catch (Exception e) {
				LOG.error("", e);
				throw new ApeException.NoNodeException(path);
			}
		}

		@Override
		public boolean appendData(byte[] data) {
			boolean error = false;
			try {
				FileOutputStream os = new FileOutputStream(file, true);
				os.write(data);
			} catch (FileNotFoundException e) {
				LOG.error("", e);
				error = true;
			} catch (IOException e) {
				LOG.error("", e);
				error = true;
			}
			return error;
		}

		@Override
		public boolean deleteData() {
			return file.delete();
		}
	}
	
	
	public static void main(String[] args) throws NodeExistsException, IOException {
		FileTracker tracker = new FileTrackerImpl();
		String path = "/data/tree/chq";
		FileTracker.FileHandler handler = tracker.createFileHandler(path);
		tracker.addFileHandler(path, handler);
		handler.appendData("haha".getBytes());
	}

	@Override
	public boolean existPath(String path) throws IOException {
		return fileHandlers.get(path) != null;
	}

	@Override
	public FileHandler getFileHandler(String path) {
		return fileHandlers.get(path);
	}
	
	
	public static void loadConfig() {
		try {
			Properties p = new Properties();
			p.load(new FileReader(new File("conf/Config")));
			rootDir = p.get("RootDir").toString();
		} catch (FileNotFoundException e) {
			LOG.error("", e);
		} catch (IOException e) {
			LOG.error("", e);
		}
	}
	

}
