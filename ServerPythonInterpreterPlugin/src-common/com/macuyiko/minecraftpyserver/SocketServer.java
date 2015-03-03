package com.macuyiko.minecraftpyserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer implements Runnable {
	private Object plugin;
	private int port;
	private String password;
	private ServerSocket listener;
	protected ExecutorService threadPool;
	
	public SocketServer (Object caller, int port, String password) {
		this.plugin = caller;
		this.port = port;
		this.password = password;
		this.threadPool = Executors.newCachedThreadPool();
	}
	
	public void run() {
		try {
			listener = new ServerSocket(port);
			Socket clientSocket ;
			while (true) {
				clientSocket = listener.accept();
				threadPool.execute(new ConnectionThread(clientSocket, this));
			}
		} catch (IOException ioe) {
			System.out.println("IOException on socket listen: " + ioe);
			ioe.printStackTrace();
		} finally {
			try {
				listener.close();
			} catch (IOException e) {}
		}

	}

	public String getPassword() {
		return password;
	}

	public ServerSocket getListener() {
		return listener;
	}

	public Object getPlugin() {
		return plugin;
	}

}
