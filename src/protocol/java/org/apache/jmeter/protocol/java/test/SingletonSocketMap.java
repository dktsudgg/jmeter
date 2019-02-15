package org.apache.jmeter.protocol.java.test;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class SingletonSocketMap {

	  private SingletonSocketMap() {}
	  public static SingletonSocketMap getInstance() {
	    return LazyHolder.INSTANCE;
	  }
	  
	  private static class LazyHolder {
	    private static final SingletonSocketMap INSTANCE = new SingletonSocketMap();  
	  }
	  
	  private ConcurrentHashMap<Long, Socket> map_ = new ConcurrentHashMap<>();
	  
	  public Socket get() {
		 Long tid = Thread.currentThread().getId();
		 if (map_.containsKey(tid)) return map_.get(tid);
		 
		 return null;
	  }
	  
	  public void set(Socket socket) {
		  Long tid = Thread.currentThread().getId();
		  if (socket == null) {
			  if (map_.containsKey(tid)) map_.remove(tid);			  
		  } else {
			  map_.put(tid,  socket);
		  }
	  }
	  
	  // close all socket
	  public void release() {
			for( Long tid : map_.keySet() ){
				try {
					map_.get(tid).close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
			}
			map_.clear();
	  }
}
